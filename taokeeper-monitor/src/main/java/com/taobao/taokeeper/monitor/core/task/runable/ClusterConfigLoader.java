package com.taobao.taokeeper.monitor.core.task.runable;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import com.taobao.taokeeper.common.GlobalInstance;
import com.taobao.taokeeper.dao.ZooKeeperClusterDAO;
import com.taobao.taokeeper.model.ZooKeeperCluster;
import common.toolkit.exception.DaoException;

/**
 * Description: Load zooKeeper cluster config info from database.
 * @author nileader / nileader@gmail.com
 * @Date Feb 16, 2012
 */
@Component
@Scope("prototype")
public class ClusterConfigLoader implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger( ClusterConfigLoader.class );

    @Autowired
    protected ZooKeeperClusterDAO zooKeeperClusterDAO;

	@Override
	public void run() {
		try {
			loadAllClusterToMemoryFromDB();
		} catch ( Throwable e ) {
			LOG.error( "Error when load zookeeper cluster config info to memeory: " + e.getMessage(),e );
		}
	}

	/**
	 * Load zookeeper cluster config info from database
	 */
	private void loadAllClusterToMemoryFromDB() throws DaoException {
		List< ZooKeeperCluster > zookeeperClusterSet = zooKeeperClusterDAO.getAllDetailZooKeeperCluster();
		if ( null == zookeeperClusterSet || zookeeperClusterSet.isEmpty() ) {
			LOG.debug( "Load all cluster to memory from DB, load 0 cluster." );
		} else {
			// First clean
			GlobalInstance.clearZooKeeperCluster();
			
			for ( ZooKeeperCluster zooKeeperCluster : zookeeperClusterSet ) { 
				GlobalInstance.putZooKeeperCluster( zooKeeperCluster.getClusterId(), zooKeeperCluster );
			}
            LOG.debug( "Load all cluster to memory from DB: " + GlobalInstance.getAllZooKeeperCluster() );
        }
	}

}
