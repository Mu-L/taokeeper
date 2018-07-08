package com.taobao.taokeeper.monitor.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description: 线程池管理
 * 
 * @author 银时 yinshi.nc@taobao.com
 * @Date Dec 25, 2011
 */
import org.springframework.stereotype.Component;

@Component
public class ThreadPoolManager {

	private static Logger LOG = LoggerFactory.getLogger( ThreadPoolManager.class );

	private static int SIZE_OF_ZKNODEALIVECHECK_EXECUTOR = 5;
	private static int SIZE_OF_MESSAGESEND_EXECUTOR = 5;
	private static final int Thread_Num_Of_Server_Monitor_Executor = 5;
	private static int SIZE_OF_ZKSERVERPERFORMAN_CECOLLECTOR_EXECUTOR = 3;
	private static int SIZE_OF_ZKCLUSTERCONFIG_DUMPER_EXECUTOR = 2;
	
	
	public void init(){
		if( null == zooKeeperNodeAliveCheckExecutor ){
			LOG.info( "Start init ThreadPoolManager..." );
			zooKeeperNodeAliveCheckExecutor 	 = Executors.newFixedThreadPool( SIZE_OF_ZKNODEALIVECHECK_EXECUTOR );
			messageSendExecutor             	 = Executors.newFixedThreadPool( SIZE_OF_MESSAGESEND_EXECUTOR );
            serverMonitorExecutor 	             = Executors.newFixedThreadPool( Thread_Num_Of_Server_Monitor_Executor );
			zkServerPerformanceCollectorExecutor = Executors.newFixedThreadPool( SIZE_OF_ZKSERVERPERFORMAN_CECOLLECTOR_EXECUTOR );
			zkClusterConfigDumperExecutor 		 = Executors.newFixedThreadPool( SIZE_OF_ZKCLUSTERCONFIG_DUMPER_EXECUTOR );
		}
	}
	
	
	
	/** 节点自检 线程池 */
	private ExecutorService zooKeeperNodeAliveCheckExecutor;
	public void addJobToZooKeeperNodeAliveCheckExecutor( Runnable command ){
		init();
		zooKeeperNodeAliveCheckExecutor.execute( command );
	}
	
	/** 消息发送 线程池 */
	private ExecutorService messageSendExecutor;
	public  void addJobToMessageSendExecutor( Runnable command ){
		init();
		messageSendExecutor.execute( command );
	}
	
	/** 收集ZKServer状态信息 线程池 */
	private static ExecutorService serverMonitorExecutor;
	public void addTaskToServerMonitorExecutor( Runnable command ){
		init();
        serverMonitorExecutor.execute( command );
	}
	
	
	/** 收集ZKServer机器信息 线程池 */
	private static ExecutorService zkServerPerformanceCollectorExecutor;
	public void addJobToZKServerPerformanceCollectorExecutor( Runnable command ){
		init();
		zkServerPerformanceCollectorExecutor.execute( command );
	}
	
	/** Dump zk cluster config info to memeory*/
	private static ExecutorService zkClusterConfigDumperExecutor;
	public void addJobToZKClusterDumperExecutor( Runnable command ){
		init();
		zkClusterConfigDumperExecutor.execute( command );
	}
	
	
	

}
