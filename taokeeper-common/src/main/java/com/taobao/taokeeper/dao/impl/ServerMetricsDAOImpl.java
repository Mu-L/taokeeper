package com.taobao.taokeeper.dao.impl;

import static common.toolkit.constant.EmptyObjectConstant.EMPTY_STRING;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.taobao.taokeeper.common.constant.SqlTemplate;
import com.taobao.taokeeper.dao.ServerMetricsDAO;
import com.taobao.taokeeper.model.ServerMetrics;
import common.toolkit.entity.DateFormat;
import common.toolkit.entity.db.DBConnectionResource;
import common.toolkit.exception.DaoException;
import common.toolkit.util.DateUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.collection.CollectionUtil;
import common.toolkit.util.db.DbcpUtil;
import org.springframework.stereotype.Repository;

/**
 * @author yinshi.nc
 * @since 2012-01-05
 */
@Repository
public class ServerMetricsDAOImpl implements ServerMetricsDAO {

	@Override
	public void insertServerMetrics(ServerMetrics serverMetrics) throws DaoException {

		if ( null == serverMetrics) {
            throw new DaoException( "Invalid server metrics:null" );
		}
		try {
			String insertSql = StringUtil.replaceSequenced( SqlTemplate.SQL_INSERT_SERVER_METRICS, serverMetrics.getClusterId() + "",
					StringUtil.trimToEmpty( serverMetrics.getServer() ),
					StringUtil.defaultIfBlank( serverMetrics.getStatDateTime(), DateUtil.getNowTime( DateFormat.DateTime ) ),
					StringUtil.defaultIfBlank( serverMetrics.getStatDate(), DateUtil.getNowTime( DateFormat.Date ) ), serverMetrics.getConnections()
							+ "", serverMetrics.getWatches() + "", serverMetrics.getSendTimes() + "", serverMetrics.getReceiveTimes() + "",
					serverMetrics.getNodeCount() + "", serverMetrics.getRwps() );

			DbcpUtil.executeInsert( insertSql );
		} catch ( Throwable e ) {
			throw new DaoException( "Error when insert into server_metrics, ServerMetrics: " + serverMetrics + ", Error: " + e.getMessage(), e );
		}
	}

	@Override
	public List<ServerMetrics> queryTaoKeeperStatByClusterIdAndServerAndStatDate(int clusterId, String server, String statDate )
			throws DaoException {

		if ( 0 == clusterId || StringUtil.isBlank( server ) || StringUtil.isBlank( statDate ) ) {
			return new ArrayList<ServerMetrics>();
		}

		List<ServerMetrics> taoKeeperStatList = new ArrayList<ServerMetrics>();

		ResultSet rs = null;
		DBConnectionResource myResultSet = null;
		try {
			myResultSet = DbcpUtil.executeQuery( StringUtil.replaceSequenced( SqlTemplate.SQL_QUERY_TAOKEEPER_STAT_BY_CLUSTERID_SERVER_DATE,
					clusterId, server, statDate ) );
			if ( null == myResultSet ) {
				return new ArrayList<ServerMetrics>();
			}
			rs = myResultSet.resultSet;
			if ( null == rs ) {
				return new ArrayList<ServerMetrics>();
			}
			while ( rs.next() ) {

				server = StringUtil.trimToEmpty( rs.getString( "server" ) );
				String statDateTime = StringUtil.trimToEmpty( StringUtil.trimToEmpty( rs.getString( "stat_date_time" ) ).replaceFirst( statDate, EMPTY_STRING ) );
				int connections 	= rs.getInt( "connections" );
				int watches 		= rs.getInt( "watches" );
				long sendTimes 		= rs.getLong( "send_times" );
				long receiveTimes 	= rs.getLong( "receive_times" );
				int nodeCount 		= rs.getInt( "node_count" );
				String rwps         = rs.getString( "rwps" );
				taoKeeperStatList.add( new ServerMetrics( clusterId, server, statDateTime, statDate, connections, watches, sendTimes, receiveTimes,
						nodeCount, rwps ) );
			}
			return taoKeeperStatList;
		} catch ( Exception e ) {
			throw new DaoException( "Error when queryTaoKeeperStatByClusterIdAndServerAndStatDate: clusterId:" + clusterId + " server:" + server
					+ "statDate:" + statDate + ", Error: " + e.getMessage(), e );
		} finally {
			if ( null != myResultSet ) {
				DbcpUtil.closeResultSetAndStatement( rs, myResultSet.statement );
				DbcpUtil.returnBackConnectionToPool( myResultSet.connection );
			}
		}

	}

	@Override
	public Map< String, List<ServerMetrics> > queryStatByClusterIdAndStatDate(int clusterId, String statDate ) throws DaoException {

		if ( 0 == clusterId || StringUtil.isBlank( statDate ) ) {
			return CollectionUtil.emptyMap();
		}

		Map< String, List<ServerMetrics> > taoKeeperStatMap = new HashMap< String, List<ServerMetrics> >();

		ResultSet rs = null;
		DBConnectionResource myResultSet = null;
		try {
			myResultSet = DbcpUtil.executeQuery( StringUtil.replaceSequenced( SqlTemplate.SQL_QUERY_TAOKEEPER_STAT_BY_CLUSTERID_DATE, clusterId,
					statDate ) );
			if ( null == myResultSet ) {
				return CollectionUtil.emptyMap();
			}
			rs = myResultSet.resultSet;
			if ( null == rs ) {
				return CollectionUtil.emptyMap();
			}
			while ( rs.next() ) {
				String server = StringUtil.trimToEmpty( rs.getString( "server" ) );
				String statDateTime = StringUtil.trimToEmpty( rs.getString( "stat_date_time" ) );
				int connections = rs.getInt( "connections" );
				int watches = rs.getInt( "watches" );
				int sendTimes = rs.getInt( "send_times" );
				int receiveTimes = rs.getInt( "receive_times" );
				int nodeCount = rs.getInt( "node_count" );
				String rwps         = rs.getString( "rwps" );
				this.addStatToTaokeeperStatMap( taoKeeperStatMap, new ServerMetrics( clusterId, server, statDateTime, statDate, connections, watches,
						sendTimes, receiveTimes, nodeCount, rwps ) );
			}

			return taoKeeperStatMap;

		} catch ( Exception e ) {
			throw new DaoException( "Error when queryTaoKeeperStatByClusterIdAndServerAndStatDate: clusterId:" + clusterId + "statDate:" + statDate
					+ ", Error: " + e.getMessage(), e );
		} finally {
			if ( null != myResultSet ) {
				DbcpUtil.closeResultSetAndStatement( rs, myResultSet.statement );
				DbcpUtil.returnBackConnectionToPool( myResultSet.connection );
			}
		}
	}

	/**
	 * Tool: Add taoKeeperStat to Map<String, List< TaoKeeperStat > >
	 * taoKeeperStatMap
	 * */
	private void addStatToTaokeeperStatMap(Map< String, List<ServerMetrics> > taoKeeperStatMap, ServerMetrics taoKeeperStat ) {

		if ( null == taoKeeperStatMap ) {
			taoKeeperStatMap = new HashMap< String, List<ServerMetrics> >();
		}

		String server = StringUtil.trimToEmpty( taoKeeperStat.getServer() );
		if ( StringUtil.isBlank( server ) )
			return;

		List<ServerMetrics> taoKeeperStatList = null;
		// The server not in keySet
		if ( null == ( taoKeeperStatList = taoKeeperStatMap.get( server ) ) ) {
			taoKeeperStatList = new ArrayList<ServerMetrics>();
			taoKeeperStatList.add( taoKeeperStat );
			taoKeeperStatMap.put( server, taoKeeperStatList );
		}
		// The server in keySet
		else {
			taoKeeperStatList.add( taoKeeperStat );
			taoKeeperStatMap.put( server, taoKeeperStatList );
		}

	}

}
