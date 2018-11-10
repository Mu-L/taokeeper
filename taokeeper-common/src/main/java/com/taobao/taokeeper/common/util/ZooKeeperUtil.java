package com.taobao.taokeeper.common.util;

import common.toolkit.util.StringUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZooKeeperUtil {


    /**
     * 解析ZooKeeper 四字命令 WCHS 的输出，Example:
     * 59 connections watching 161 paths
     * Total watches:405
     * 提取 connections、paths 和 watches 的数量。
     *
     * @param str1 格式必须为 "X connections watching Y paths"
     * @param str2 格式必须为 "Total watches:Z"
     * @return int[3] 数组，其中：<br/>
     *         - [0] = connections 连接数<br/>
     *         - [1] = watchedPaths 被Watch的Path数量<br/>
     *         - [2] = watches 注册在ZooKeeper服务器上的Watch数量<br/>
     * @throws IllegalArgumentException 如果任一字符串格式不匹配
     */
    public static int[] parseCommondOfWchs(String str1, String str2) throws IllegalArgumentException{
        // 定义正则表达式
        Pattern pattern1 = Pattern.compile("^(\\d+)\\s+connections\\s+watching\\s+(\\d+)\\s+paths$");
        Pattern pattern2 = Pattern.compile("^Total\\s+watches:(\\d+)$");

        // 匹配第一个字符串
        Matcher matcher1 = pattern1.matcher(str1);
        if (!matcher1.matches()) {
            throw new IllegalArgumentException("Invalid format for first string: \"" + str1 + "\". Expected format: \"X connections watching Y paths\"");
        }

        // 匹配第二个字符串
        Matcher matcher2 = pattern2.matcher(str2);
        if (!matcher2.matches()) {
            throw new IllegalArgumentException("Invalid format for second string: \"" + str2 + "\". Expected format: \"Total watches:Z\"");
        }

        // 提取数值
        int connections = Integer.parseInt(matcher1.group(1));
        int watchedPaths = Integer.parseInt(matcher1.group(2));
        int watches = Integer.parseInt(matcher2.group(1));

        return new int[]{connections, watchedPaths, watches};
    }


    /** 分析stat命令的一行内容, 判断是否为客户端连接
     * 标准的一行客户端连接是这样的
     * "/1.2.37.111:43681[1](queued=0,recved=434,sent=434)"
     * */
    public static boolean analyseLineIfClientConnection( String input ) {
        if ( StringUtil.isBlank( input ) ) {
            return false;
        }

        input = StringUtil.trimToEmpty( input );
        if ( input.startsWith( "/" ) && StringUtil.containsIp( input ) ) {
            return true;
        }
        return false;
    }





}

