package com.taobao.taokeeper.model;


/**
 * Model: Metrics of a server
 * @author   yinshi.nc nileader@gmail.com
 */
public class ServerMetrics {

	private int clusterId;
	private String server;
	private String statDateTime;
	private String statDate;
	private int connections;
	private int watches;
	private long sendTimes;
	private long receiveTimes;
	private long nodeCount;
	private String rwps;

	public ServerMetrics(){}
	public ServerMetrics(int clusterId, String server, String statDateTime, String statDate, int connections, int watches, long sendTimes, long receiveTimes, long nodeCount, String rwps ){
		this.clusterId    = clusterId;
		this.server       = server;
		this.statDateTime = statDateTime;
		this.statDate     = statDate;
		this.connections  = connections;
		this.watches      = watches;
		this.sendTimes    = sendTimes;
		this.receiveTimes = receiveTimes;
		this.nodeCount    = nodeCount;
		this.rwps         = rwps;
	}
	

	public int getClusterId() {
		return clusterId;
	}
	public void setClusterId( int clusterId ) {
		this.clusterId = clusterId;
	}
	public String getServer() {
		return server;
	}
	public void setServer( String server ) {
		this.server = server;
	}
	public String getStatDateTime() {
		return statDateTime;
	}
	public void setStatDateTime( String statDateTime ) {
		this.statDateTime = statDateTime;
	}
	public String getStatDate() {
		return statDate;
	}
	public void setStatDate( String statDate ) {
		this.statDate = statDate;
	}
	public int getWatches() {
		return watches;
	}
	public void setWatches( int watches ) {
		this.watches = watches;
	}
	public long getSendTimes() {
		return sendTimes;
	}
	public void setSendTimes( int sendTimes ) {
		this.sendTimes = sendTimes;
	}
	public long getReceiveTimes() {
		return receiveTimes;
	}
	public void setReceiveTimes( int receiveTimes ) {
		this.receiveTimes = receiveTimes;
	}
	public long getNodeCount() {
		return nodeCount;
	}
	public void setNodeCount( int nodeCount ) {
		this.nodeCount = nodeCount;
	}
	public int getConnections() {
		return connections;
	}
	public void setConnections( int connections ) {
		this.connections = connections;
	}
	public String getRwps() {
		return rwps;
	}
	public void setRwps( String rwps ) {
		this.rwps = rwps;
	}

    public String toString4PrometheusMetric() {
        StringBuilder sb = new StringBuilder();

        // connections: 当前连接数 -> gauge
        sb.append("# HELP zookeeper_connections Current number of client connections\n");
        sb.append("# TYPE zookeeper_connections gauge\n");
        sb.append("zookeeper_connections ").append(connections).append("\n\n");

        // watches: 当前 watches 数量 -> gauge
        sb.append("# HELP zookeeper_watches Current number of watches\n");
        sb.append("# TYPE zookeeper_watches gauge\n");
        sb.append("zookeeper_watches ").append(watches).append("\n\n");

        // sendTimes: 累计发送次数 -> counter
        sb.append("# HELP zookeeper_packets_sent_total Total number of packets sent\n");
        sb.append("# TYPE zookeeper_packets_sent_total counter\n");
        sb.append("zookeeper_packets_sent_total ").append(sendTimes).append("\n\n");

        // receiveTimes: 累计接收次数 -> counter
        sb.append("# HELP zookeeper_packets_received_total Total number of packets received\n");
        sb.append("# TYPE zookeeper_packets_received_total counter\n");
        sb.append("zookeeper_packets_received_total ").append(receiveTimes).append("\n\n");

        // nodeCount: znode 总数 -> gauge
        sb.append("# HELP zookeeper_znodes Total number of znodes\n");
        sb.append("# TYPE zookeeper_znodes gauge\n");
        sb.append("zookeeper_znodes ").append(nodeCount).append("\n\n");

        // 注意：rwps 是字符串，无法转为数值指标，故跳过

        return sb.toString();
    }
	
}
