package com.taobao.taokeeper.monitor.core.task;

import static com.taobao.taokeeper.common.constant.SystemConstant.MINS_RATE_OF_COLLECT_ZOOKEEPER;
import static common.toolkit.constant.SymbolConstant.COLON;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.taobao.taokeeper.monitor.core.task.runable.ServerMonitorTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.taobao.taokeeper.common.GlobalInstance;
import com.taobao.taokeeper.dao.AlarmSettingsDAO;
import com.taobao.taokeeper.dao.ZooKeeperClusterDAO;
import com.taobao.taokeeper.model.AlarmSettings;
import com.taobao.taokeeper.model.ZooKeeperCluster;
import com.taobao.taokeeper.monitor.core.ThreadPoolManager;
import common.toolkit.exception.DaoException;
import common.toolkit.util.DateUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.ThreadUtil;
/**
 * All ZooKeeper cluster monitor<br/>
 *cluster1<br/>
 *--server1</br>
 *--server2</br>
 *cluster2</br>
 *--server1</br>
 *--server2</br>
 *...
 * @author yinshi.nc
 * @Date 2011-10-28
 */
@Component
public class ZooKeeperStatusMonitor implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger( ZooKeeperStatusMonitor.class );

	private boolean isFirst = true;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ThreadPoolManager threadPoolManager;
    @Autowired
    ZooKeeperClusterDAO zooKeeperClusterDAO;
    @Autowired
    AlarmSettingsDAO alarmSettingsDAO;
	
	@Override
	public void run() {

		while ( true ) {
			
			if( !GlobalInstance.need_zk_status_collect ){
				LOG.info( "Not collect zookeeper status, need_zk_status_collect=" + GlobalInstance.need_zk_status_collect );
				ThreadUtil.sleep( 1000 * 60 * MINS_RATE_OF_COLLECT_ZOOKEEPER );
				continue;
			}

            List< ZooKeeperCluster > zooKeeperClusterSet = null;
            Map< Integer, ZooKeeperCluster > zooKeeperClusterMap = null;
            try {
                zooKeeperClusterMap = zooKeeperClusterDAO.getAllCluster();
            } catch (DaoException e) {
                LOG.error( "Error when get all cluster from db due to " + e.getMessage(),e );
            }

            if ( null == zooKeeperClusterMap || zooKeeperClusterMap.isEmpty() ) {
                LOG.info( "Skip server monitor due to no zookeeper cluster" );
            } else {
                for ( Integer clusterId : zooKeeperClusterMap.keySet() ) { // handle each cluster

                    ZooKeeperCluster zookeeperCluster = zooKeeperClusterMap.get(clusterId);
                    if ( null != zookeeperCluster && null != zookeeperCluster.getServerList() ) {

                        for ( String server : zookeeperCluster.getServerList() ) { //handle each server
                            if ( StringUtil.isBlank( server ) )
                                continue;
                            String ip = StringUtil.trimToEmpty( server.split( COLON )[0] );
                            String port = StringUtil.trimToEmpty( server.split( COLON )[1] );


                            ServerMonitorTask _serverMonitorTask = applicationContext.getBean(ServerMonitorTask.class);
                            _serverMonitorTask.setClusterId(clusterId);
                            _serverMonitorTask.setIp(ip);
                            _serverMonitorTask.setPort(port);

                            threadPoolManager.addTaskToServerMonitorExecutor(_serverMonitorTask);
                        }
                    }
                }
            }
            LOG.info( "Finish all cluster monitor" );
            GlobalInstance.timeOfUpdateZooKeeperStatusSet = DateUtil.convertDate2String( new Date() );

            try {
                Thread.sleep( 1000 * 60 * MINS_RATE_OF_COLLECT_ZOOKEEPER );
            } catch (InterruptedException e) {
                //
            }
        }
	}

	


	


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
