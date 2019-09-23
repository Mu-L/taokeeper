package com.taobao.taokeeper.monitor.service;

/**
 * Description: Metric service of ZooKeeper
 * @author   yinshi.nc
 * @since 2012-01-05
 */
public interface ReportService {
	
	/** generate the content of server conns of each ip in cluster 
	 * @throws Exception 
	 * */
	public String getReportContentOfServerConnectionByClusterIdAndServerAndStatDate( int clusterId, String server, String statDate ) throws Exception;
	
}
