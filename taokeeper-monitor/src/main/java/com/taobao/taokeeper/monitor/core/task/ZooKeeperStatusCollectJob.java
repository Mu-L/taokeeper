package com.taobao.taokeeper.monitor.core.task;

import static com.taobao.taokeeper.common.constant.SystemConstant.MINS_RATE_OF_COLLECT_ZOOKEEPER;
import static common.toolkit.constant.SymbolConstant.COLON;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import com.taobao.taokeeper.monitor.core.task.runable.ServerMonitorTask;
import common.toolkit.exception.DaoException;
import common.toolkit.util.DateUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.ThreadUtil;
/**
 * Description: Collect info of zookeeper by jmx.
 * 
 * @author yinshi.nc
 * @Date 2011-10-28
 */
@Component
public class ZooKeeperStatusCollectJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger( ZooKeeperStatusCollectJob.class );

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

			try {
				AlarmSettings alarmSettings = null;
				try {
					List< ZooKeeperCluster > zooKeeperClusterSet = null;
					Map< Integer, ZooKeeperCluster > zooKeeperClusterMap = GlobalInstance.getAllZooKeeperCluster();
					if ( null == zooKeeperClusterMap ) {
						zooKeeperClusterSet = zooKeeperClusterDAO.getAllDetailZooKeeperCluster();
					} else {
						zooKeeperClusterSet = new ArrayList< ZooKeeperCluster >();
						zooKeeperClusterSet.addAll( zooKeeperClusterMap.values() );
					}

					if ( null == zooKeeperClusterSet || zooKeeperClusterSet.isEmpty() ) {
						LOG.warn( "No zookeeper cluster" );
					} else {
						for ( ZooKeeperCluster zookeeperCluster : zooKeeperClusterSet ) { // 对每个cluster处理
							alarmSettings = alarmSettingsDAO.getAlarmSettingsByCulsterId( zookeeperCluster.getClusterId() );
							if ( null != zookeeperCluster && null != zookeeperCluster.getServerList() ) {

								for ( String server : zookeeperCluster.getServerList() ) {
									if ( StringUtil.isBlank( server ) )
										continue;
									String ip = StringUtil.trimToEmpty( server.split( COLON )[0] );
									String port = StringUtil.trimToEmpty( server.split( COLON )[1] );
									
									//这里插入一个任务
									if( isFirst ){
										ThreadPoolManager.addJobToZKServerStatusCollectorExecutor( new ServerMonitorTask( ip, port, alarmSettings, zookeeperCluster, false ) );
										isFirst = false;
									}else{
										ThreadPoolManager.addJobToZKServerStatusCollectorExecutor( new ServerMonitorTask( ip, port, alarmSettings, zookeeperCluster, true ) );
									}
								}// for each server
							}// for each cluster
						}
					}
					LOG.info( "Finish all cluster status collect" );
					GlobalInstance.timeOfUpdateZooKeeperStatusSet = DateUtil.convertDate2String( new Date() );
				} catch ( DaoException daoException ) {
					LOG.warn( "Error when handle data base" + daoException.getMessage() );
				} catch ( Exception e ) {
					LOG.error( "程序出错:" + e.getMessage() );
					e.printStackTrace();
				}
				// 每2分钟收集一次检测
				Thread.sleep( 1000 * 60 * MINS_RATE_OF_COLLECT_ZOOKEEPER );
			} catch ( Throwable e ) {
				e.printStackTrace();
			}
		}
	}

	


	


	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
