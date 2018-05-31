package com.taobao.taokeeper.monitor.core.task;
import static com.taobao.taokeeper.common.constant.SystemConstant.MINS_RATE_OF_DUMP_ZOOKEEPER_CLUSTER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.taobao.taokeeper.monitor.core.ThreadPoolManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


/**
 * Description:Dump ZooKeeper cluster info to memory
 * @author  nileader / nileader@gmail.com
 * @Date	 Feb 16, 2012
 */
@Component
public class ZooKeeperClusterMapDumpJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger( ZooKeeperClusterMapDumpJob.class );

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private ThreadPoolManager threadPoolManager;

	@Override
	public void run() {
		
		while( true ){
			
			try{
                ClusterConfigLoader newLoader = applicationContext.getBean(ClusterConfigLoader.class);
                threadPoolManager.addJobToZKClusterDumperExecutor(newLoader);

                Thread.sleep( 1000 * 60 * MINS_RATE_OF_DUMP_ZOOKEEPER_CLUSTER );
			}catch( Throwable e ){
				LOG.error( "Error when dump zk cluster config info to memory: " + e.getMessage() );
				e.printStackTrace();
			}
		}
	}
	
}
