package com.taobao.taokeeper.dao;
import java.util.List;
import java.util.Map;

import com.taobao.taokeeper.model.ServerMetrics;
import common.toolkit.exception.DaoException;

/**
 * Server Metrics dao
 * @author   yinshi.nc nileader@gmail.com
 * @since 2012-01-05
 */
public interface ServerMetricsDAO {
	
	/**
     * Insert a server mertics into db
     */
	public void insertServerMetrics(ServerMetrics serverMetrics) throws DaoException;
	
	
	/**
	 * 根据cluster_id, server, stat_date 来查询一个统计信息
	 * @param clusterId
	 * @param server	ip
	 * @param statDate	2012-01-05
	 * @throws DaoException
	 */
	public List<ServerMetrics> queryTaoKeeperStatByClusterIdAndServerAndStatDate(int clusterId, String server, String statDate ) throws DaoException;
	
	
	/**
	 * 根据cluster_id, stat_date 来查询一个集群的统计信息
	 * @param clusterId
	 * @param statDate	2012-01-05
	 * @throws DaoException
	 */
	public Map<String, List<ServerMetrics> > queryStatByClusterIdAndStatDate(int clusterId, String statDate ) throws DaoException;
	
	
}
