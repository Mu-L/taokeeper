package com.taobao.taokeeper.monitor;

import com.taobao.taokeeper.common.SystemInfo;
import com.taobao.taokeeper.common.constant.SystemConstant;
import com.taobao.taokeeper.monitor.core.ThreadPoolManager;
import com.taobao.taokeeper.monitor.core.task.ZooKeeperStatusMonitor;
import common.toolkit.util.ObjectUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.ThreadUtil;
import common.toolkit.util.db.DbcpUtil;
import common.toolkit.util.number.IntegerUtil;
import common.toolkit.util.system.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Properties;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.taobao.taokeeper.monitor",
        "com.taobao.taokeeper.dao.impl",
        "com.taobao.taokeeper.monitor.web"
})
public class TaokeeperMonitorApplication {
    private static final Logger LOG = LoggerFactory.getLogger( TaokeeperMonitorApplication.class );

    private static ZooKeeperStatusMonitor staticZooKeeperStatusMonitor;
    private ZooKeeperStatusMonitor zooKeeperStatusMonitor;

    @Autowired
    public void setZooKeeperStatusMonitor(ZooKeeperStatusMonitor zooKeeperStatusMonitor){
        staticZooKeeperStatusMonitor = zooKeeperStatusMonitor;
    }


    public static void main(String[] args) {

        SpringApplication.run(TaokeeperMonitorApplication.class, args);
        initSystem();
    }


    /**
     * 从数据库加载并初始化系统配置
     */
    static void initSystem() {
        LOG.info( "=================================Start to init system===========================" );
        Properties properties = null;
        try {
            properties = SystemUtil.loadProperty();
            if ( ObjectUtil.isBlank( properties ) )
                throw new Exception( "Please defined,such as -DconfigFilePath=\"W:\\TaoKeeper\\taokeeper\\config\\config-test.properties\"" );
        } catch ( Exception e ) {
            LOG.error( e.getMessage() );
            throw new RuntimeException( e.getMessage(), e.getCause() );
        }

        SystemInfo.envName = StringUtil.defaultIfBlank( properties.getProperty( "systemInfo.envName" ), "TaoKeeper-Deploy" );

        DbcpUtil.driverClassName = StringUtil.defaultIfBlank( properties.getProperty( "dbcp.driverClassName" ), "com.mysql.jdbc.Driver" );
        DbcpUtil.dbJDBCUrl = StringUtil.defaultIfBlank( properties.getProperty( "dbcp.dbJDBCUrl" ), "jdbc:mysql://127.0.0.1:3306/taokeeper" );
        DbcpUtil.characterEncoding = StringUtil.defaultIfBlank( properties.getProperty( "dbcp.characterEncoding" ), "UTF-8" );
        DbcpUtil.username = StringUtil.trimToEmpty( properties.getProperty( "dbcp.username" ) );
        DbcpUtil.password = StringUtil.trimToEmpty( properties.getProperty( "dbcp.password" ) );
        DbcpUtil.maxActive = IntegerUtil.defaultIfError( properties.getProperty( "dbcp.maxActive" ), Integer.valueOf(30));
        DbcpUtil.maxIdle = IntegerUtil.defaultIfError( properties.getProperty( "dbcp.maxIdle" ), Integer.valueOf(10));
        DbcpUtil.maxWait = IntegerUtil.defaultIfError( properties.getProperty( "dbcp.maxWait" ), Integer.valueOf(10000));

        SystemConstant.dataStoreBasePath = StringUtil.defaultIfBlank( properties.getProperty( "SystemConstent.dataStoreBasePath" ),
                "/home/yinshi.nc/taokeeper-monitor/" );
        SystemConstant.userNameOfSSH = StringUtil.defaultIfBlank( properties.getProperty( "SystemConstant.userNameOfSSH" ), "admin" );
        SystemConstant.passwordOfSSH = StringUtil.defaultIfBlank( properties.getProperty( "SystemConstant.passwordOfSSH" ), "123456" );
        SystemConstant.portOfSSH = IntegerUtil.defaultIfError( properties.getProperty( "SystemConstant.portOfSSH" ), Integer.valueOf(22));

        SystemConstant.IP_OF_MESSAGE_SEND = StringUtil.trimToEmpty( properties.getProperty( "SystemConstant.IP_OF_MESSAGE_SEND" ) );


        LOG.info( "=================================Finish init system===========================" );
//        ThreadPoolManager.addJobToMessageSendExecutor( new TbMessageSender( new Message( "银时", "TaoKeeper启动", "TaoKeeper启动",
//                Message.MessageType.WANGWANG ) ) );


        /** 启动ZooKeeper集群状态收集 */
        ThreadUtil.startThread( staticZooKeeperStatusMonitor );

    }
}