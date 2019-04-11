package com.taobao.taokeeper.monitor.core.task.runable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.taobao.taokeeper.common.GlobalInstance;
import com.taobao.taokeeper.common.constant.SystemConstant;
import com.taobao.taokeeper.common.util.ZooKeeperUtil;
import com.taobao.taokeeper.dao.ReportDAO;
import com.taobao.taokeeper.model.*;
import common.toolkit.entity.DateFormat;
import common.toolkit.entity.io.Connection;
import common.toolkit.exception.DaoException;
import common.toolkit.exception.SSHException;
import common.toolkit.util.DateUtil;
import common.toolkit.util.ObjectUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.collection.MapUtil;
import common.toolkit.util.io.SSHUtil;
import common.toolkit.util.io.SocketCommandUtils;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.taobao.taokeeper.common.constant.SystemConstant.*;
import static common.toolkit.constant.BaseConstant.WORD_SEPARATOR;
import static common.toolkit.constant.EmptyObjectConstant.EMPTY_STRING;
import static common.toolkit.constant.HtmlTagConstant.BR;


/**
 * Description: Monitor task for a server include:
 *
 * @author 银时(nileader) yinshi.nc@taobao.com
 * @Date Dec 26, 2019
 */
public class ServerMonitorTask implements Runnable{

    private static final Logger LOG = LoggerFactory.getLogger( ServerMonitorTask.class );

    private static final String MODE_FOLLOWER = "Mode: follower";
    private static final String MODE_LEADERER = "Mode: leader";
    private static final String MODE_STANDALONE = "Mode: standalone";
    private static final String MODE_OBSERVER = "Mode: observer";
    private static final String NODE_COUNT = "Node count:";

    private static final String STRING_CONNECTIONS_WATCHING = "connections watching";
    private static final String STRING_PATHS = "paths";
    private static final String STRING_TOTAL_WATCHES = "Total watches:";

    private static final String STRING_SENT = "Sent:";

    private static final String STRING_RECEIVED = "Received:";

    private String ip;
    private String port;
    private boolean needStoreToDB;

    public ServerMonitorTask(String ip, String port) {
        this.ip = ip;
        this.port = port;
        this.needStoreToDB = true;
    }
    public ServerMonitorTask(String ip, String port, boolean needStoreToDB ) {
        this.ip = ip;
        this.port = port;
        this.needStoreToDB = needStoreToDB;
    }

    @Override
    public void run() {

            if ( StringUtil.isBlank( ip, port + EMPTY_STRING ) ) {
                LOG.warn( "IP or port is empty." );
                return;
            }
            String server = ip+":"+port;

            ZooKeeperStatusV2 zookeeperStatus = new ZooKeeperStatusV2();
            zookeeperStatus.setServer(server);

            try{
                handleStat( ip, Integer.parseInt( port ), zookeeperStatus );
                LOG.info("Finish handle ZooKeeper Command[stat] @"+ip+":"+port);
            }catch ( Throwable e ){
                LOG.error("Exception when handle ZooKeeper Command[stat] @"+ip+":"+port,e);
            }

            try{
                handleWchs( ip, Integer.parseInt( port ), zookeeperStatus );
                LOG.info("Finish handle ZooKeeper Command[wchs] @"+ip+":"+port);
            }catch ( Throwable e ){
                LOG.error("Exception when handle ZooKeeper Command[wchs] @"+ip+":"+port,e);
            }

            try{
                handleWchc( ip, Integer.parseInt( port ), zookeeperStatus );
                LOG.info("Finish handle ZooKeeper Command[wchc] @"+ip+":"+port);
            }catch ( Throwable e ){
                LOG.error("Exception when handle ZooKeeper Command[wchc] @"+ip+":"+port,e);
            }

            try{
                GlobalInstance.putSelfCheckResult(server,0);
                GlobalInstance.putSelfCheckResult( server,handleSelfCheck(ip,Integer.parseInt( port )) );
                LOG.info("Finish handle ZooKeeper self check @"+server);
            }catch ( Throwable e ){
                GlobalInstance.putSelfCheckResult(server,2);
                LOG.error("Exception when handle ZooKeeper Command[wchc] @"+ip+":"+port,e);
            }

            GlobalInstance.putZooKeeperStatus( ip+":"+port, zookeeperStatus );

            //final Map< String, Connection > consOfServer = GlobalInstance.getZooKeeperClientConnectionMapByClusterIdAndServerIp( ip );
            //zookeeperStatus.setConnections( consOfServer );

            //checkAndAlarm( alarmSettings, zooKeeperStatus, zookeeperCluster.getClusterName() );

            LOG.info( "Finish all monitor item check of " + ip + ":" + port );

    }

    /**
     * Exec 4 words command: stat
     */
    private void handleStat(String ip, int port, ZooKeeperStatus zooKeeperStatus ) throws IOException {

        InputStream is = null;
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        try{
            is = SocketCommandUtils.executeSocketCommandAsStream(ip,port,COMMAND_STAT);
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            /**
             * 通常的内容是这样： Zookeeper version: 3.3.3-1073969, built on 02/23/2011
             * 22:27 GMT Clients:
             * /1.2.37.111:43681[1](queued=0,recved=434,sent=434)
             * /10.13.44.47:54811[0](queued=0,recved=1,sent=0)
             *
             * Latency min/avg/max: 0/1/227 Received: 2349 Sent: 2641
             * Outstanding: 0 Zxid: 0xc00000243 Mode: follower Node count: 8
             */
            List< String > clientConnectionList = new ArrayList< String >();
            while ((line = reader.readLine()) != null) {
                if ( ZooKeeperUtil.analyseLineIfClientConnection( line ) ) { // 检查是否是客户端连接
                    clientConnectionList.add( line );
                } else if ( line.contains( MODE_FOLLOWER ) ) {
                    zooKeeperStatus.setMode( "F" );
                } else if ( line.contains( MODE_LEADERER ) ) {
                    zooKeeperStatus.setMode( "L" );
                } else if ( line.contains( MODE_STANDALONE ) ) {
                    zooKeeperStatus.setMode( "S" );
                } else if ( line.contains( MODE_OBSERVER ) ) {
                    zooKeeperStatus.setMode( "O" );
                }else if ( line.contains( NODE_COUNT ) ) {
                    zooKeeperStatus.setNodeCount( Integer.parseInt( StringUtil.trimToEmpty( line.replace( NODE_COUNT, EMPTY_STRING ) ) ) );
                } else if ( line.contains( STRING_SENT ) ) {
                    zooKeeperStatus.setSent( StringUtil.trimToEmpty( line.replace( STRING_SENT, EMPTY_STRING ) ) );
                } else if ( line.contains( STRING_RECEIVED ) ) {
                    zooKeeperStatus.setReceived( StringUtil.trimToEmpty( line.replace( STRING_RECEIVED, EMPTY_STRING ) ) );
                }
                sb.append( line ).append( "<br/>" );
            }
            //已经获取了stat命令的返回值，检查下内容是否正确
            zooKeeperStatus.setClientConnectionList( clientConnectionList );
            zooKeeperStatus.setStatContent( sb.toString() );
        } finally {
            if(null!=is) is.close();
            if(null!=reader) reader.close();
        }
    }

    /**
     * Exec 4 words command: wchs
     */
    private void handleWchs(String ip, int port, ZooKeeperStatus zookeeperStatus ) throws IOException {

        InputStream is = null;
        BufferedReader reader = null;
        try{
            is = SocketCommandUtils.executeSocketCommandAsStream(ip,port,COMMAND_WCHS);
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            int[] result = ZooKeeperUtil.parseCommondOfWchs(reader.readLine(),reader.readLine());

            zookeeperStatus.setWatchedPaths(result[1]);
            zookeeperStatus.setWatches(result[2]);

        } finally {
            if(null!=is) is.close();
            if(null!=reader) reader.close();
        }
    }

    /**
     * Exec 4 words command: wchc
     */
    private void handleWchc(String ip, int port, ZooKeeperStatus zookeeperStatus ) throws IOException {

        InputStream is = null;
        BufferedReader reader = null;
        try{
            is = SocketCommandUtils.executeSocketCommandAsStream(ip,port,COMMAND_WCHC);
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            Map<String, List<String>> watchedPathMap= ZooKeeperUtil.parseCommondOfWchc(reader);

            zookeeperStatus.setWatchedPathMap(watchedPathMap);
            zookeeperStatus.setWatchedPathMapContent( MapUtil.toString(watchedPathMap));

        } finally {
            if(null!=is) is.close();
            if(null!=reader) reader.close();
        }
    }

    /**
     * 执行自检操作，验证与ZooKeeper服务器的连接及基本读写功能<br/>
     * @return 如果所有步骤都成功完成则返回1，否则返回2
     */
    private int handleSelfCheck(String ip, int port) {
        final String selfCheckPath = "/YINSHI.MONITOR.ALIVE.CHECK-"+ip+":"+port+"-"+System.currentTimeMillis();
        final String[] receivedData = {null};
        final boolean[] dataChanged = {false};
        ZkClient zkClient = null;

        try {
            // 步骤1: 使用zkclient建立和zookeeper一台服务器的连接
            zkClient = new ZkClient(ip + ":" + port, 10000);

            // 步骤2: 删除一个固定路径的节点（如果存在）
            if (zkClient.exists(selfCheckPath)) {
                zkClient.delete(selfCheckPath);
            }

            // 步骤3: 检查节点是否存在，预期：不存在
            if (zkClient.exists(selfCheckPath)) {
                LOG.warn("Failed to delete node: " + selfCheckPath + " on "+ip+":"+port);
                return 2;
            }

            // 步骤4: 创建节点
            zkClient.create(selfCheckPath, "", CreateMode.EPHEMERAL);

            // 步骤5: 检查节点是否存在，预期：存在
            if (!zkClient.exists(selfCheckPath)) {
                LOG.warn("Failed to create node: " + selfCheckPath + " on "+ip+":"+port);
                return 2;
            }

            // 步骤6: 在节点上创建监听
            zkClient.subscribeDataChanges(selfCheckPath, new IZkDataListener() {
                public void handleDataChange(String dataPath, Object data) throws Exception {
                    receivedData[0] = data.toString();
                    dataChanged[0] = true;
                }

                public void handleDataDeleted(String dataPath) throws Exception {
                    // ignore
                }
            });

            // 步骤7: 在节点写入数据
            String testData = String.valueOf(System.currentTimeMillis());
            zkClient.writeData(selfCheckPath, testData);

            // 等待数据变更通知
            int waitCount = 0;
            while (!dataChanged[0] && waitCount < 10) { // 最多等待10秒
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // ignore
                }
                waitCount++;
            }

            // 步骤8: 检查收到的通知和数据是否正确
            if (!dataChanged[0]) {
                LOG.warn("Did not receive data change notification" + " on "+ip+":"+port);
                return 2;
            }

            if (!testData.equals(receivedData[0])) {
                LOG.warn("Data mismatch. Expected: " + testData + ", but got: " + receivedData[0] + " on "+ip+":"+port);
                return 2;
            }

            return 1;

        }finally {
            // 关闭zkClient连接
            if (zkClient != null) zkClient.close();
        }
    }







    // 检查并进行报警
    private void checkAndAlarm( AlarmSettings alarmSettings, ZooKeeperStatus zooKeeperStatus, String clusterName ) {

        if ( null == alarmSettings )
            return;

        try {
            boolean needAlarm = false;
            StringBuilder sb = new StringBuilder();
            String maxConnectionPerIp = StringUtil.trimToEmpty( alarmSettings.getMaxConnectionPerIp() );
            String maxWatchPerIp = StringUtil.trimToEmpty( alarmSettings.getMaxWatchPerIp() );

            if ( !StringUtil.isBlank( maxConnectionPerIp ) ) {
                Map< String, Connection > conns = zooKeeperStatus.getConnections();
                int connectionsPerIp = 0;
                if ( null != conns )
                    connectionsPerIp = conns.size();

                if ( Integer.parseInt( maxConnectionPerIp ) < connectionsPerIp ) {
                    needAlarm = true;
                    sb.append( zooKeeperStatus.getServer() + " 上的连接数达到了: " + connectionsPerIp + ", 超过设置的报警阀值: " + maxConnectionPerIp + ".  " );
                }
            }

            if ( !StringUtil.isBlank( maxWatchPerIp ) ) {
                int watchesPerIp = zooKeeperStatus.getWatches();
                if ( Integer.parseInt( maxWatchPerIp ) < watchesPerIp ) {
                    needAlarm = true;
                    sb.append( zooKeeperStatus.getServer() + " 上的Watch数达到了: " + watchesPerIp + ", 超过设置的报警阀值: " + maxWatchPerIp + ".  " );
                }
            }

            if ( needAlarm ) {
                LOG.warn( "ZooKeeper连接数，Watcher数报警" + sb.toString() );
                if ( GlobalInstance.needAlarm.get() ) {
                    String wangwangList = alarmSettings.getWangwangList();
                    String phoneList = alarmSettings.getPhoneList();

                    //ThreadPoolManager.addJobToMessageSendExecutor( new TbMessageSender( new Message( wangwangList, "ZooKeeper连接数，Watcher数报警-" + clusterName, clusterName + "-" + sb.toString(), Message.MessageType.WANGWANG ) ) );
                    //ThreadPoolManager.addJobToMessageSendExecutor( new TbMessageSender( new Message( phoneList, "ZooKeeper连接数，Watcher数报警-" + clusterName, clusterName + "-" + sb.toString(), Message.MessageType.WANGWANG ) ) );
                }
            }// need alarm
        } catch ( NumberFormatException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }




    /**
     * store taokeeper stat to DB
     * TODO 这个方法要异步处理
     * @param clusterId
     * @param zooKeeperStatus
     */
    private void storeTaoKeeperStatToDB( int clusterId, ZooKeeperStatusV2 zooKeeperStatus ) {

        try {
            WebApplicationContext wac = ContextLoader.getCurrentWebApplicationContext();
            ReportDAO reportDAO = ( ReportDAO ) wac.getBean( "reportDAO" );

            TypeToken< ZooKeeperStatusV2.RWStatistics > type = new TypeToken< ZooKeeperStatusV2.RWStatistics >() {
            };

            String rwStatistics = "";
            if ( !ObjectUtil.isBlank( zooKeeperStatus.getRwps()) ) {
                rwStatistics = new Gson().toJson( zooKeeperStatus.getRwps(), type.getType() );
            }

            reportDAO.addTaoKeeperStat( new TaoKeeperStat( clusterId,
                    zooKeeperStatus.getServer(),
                    DateUtil.getNowTime( DateFormat.DateTime ),
                    DateUtil.getNowTime( DateFormat.Date ),
                    MapUtil.size( zooKeeperStatus.getConnections() ),
                    zooKeeperStatus.getWatches(),
                    Long.parseLong( zooKeeperStatus.getSent() ),
                    Long.parseLong( zooKeeperStatus.getReceived() ),
                    zooKeeperStatus.getNodeCount(), rwStatistics ) );
        } catch ( NumberFormatException e ) {
            LOG.error( "将统计信息记入数据库出错：" + e.getMessage() );
            e.printStackTrace();
        } catch ( DaoException e ) {
            LOG.error( "将统计信息记入数据库出错：" + e.getMessage() );
            e.printStackTrace();
        } catch ( Exception e ) {
            LOG.error( "将统计信息记入数据库出错：" + e.getMessage() );
            e.printStackTrace();
        }
    }
















}