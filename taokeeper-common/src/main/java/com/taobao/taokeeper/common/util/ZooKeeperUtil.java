package com.taobao.taokeeper.common.util;

import common.toolkit.util.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZooKeeperUtil {

    private static final Pattern SESSION_ID_PATTERN_OF_WCHC = Pattern.compile("^0x[0-9a-fA-F]+$");
    private static final Pattern ZNODE_PATH_PATTERN_OF_WCHC = Pattern.compile("^/[^\r\n\t ]*$");




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

    /**
     * 从 BufferedReader 流式解析 wchc 命令输出。
     *
     * @param reader 已连接的 BufferedReader（本方法不会关闭，需要调用方主动关闭）
     * @return Map<sessionId, List<path>>
     * @throws IOException          if I/O error occurs
     * @throws IllegalArgumentException if format is invalid
     */
    public static Map<String, List<String>> parseCommondOfWchc(BufferedReader reader) throws IOException {

        Map<String, List<String>> result = new LinkedHashMap<>();
        String currentSessionId = null;
        int lineNumber = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;

            // 跳过空行
            if (line.trim().isEmpty()) {
                continue;
            }

            boolean isPathLine = !line.isEmpty() && (line.charAt(0) == ' ' || line.charAt(0) == '\t');

            if ( !isPathLine ) {
                // 会话 ID 行
                String sessionId = line.trim();
                if (!SESSION_ID_PATTERN_OF_WCHC.matcher(sessionId).matches()) {
                    throw new IllegalArgumentException(
                            String.format("Invalid session ID at line %d: \"%s\"", lineNumber, line)
                    );
                }
                if (result.containsKey(sessionId)) {
                    throw new IllegalArgumentException(
                            String.format("Duplicate session ID at line %d: %s", lineNumber, sessionId)
                    );
                }
                currentSessionId = sessionId;
                result.put(currentSessionId, new ArrayList<>());
            } else {
                // 路径行
                if (currentSessionId == null) {
                    throw new IllegalArgumentException(
                            String.format("Path line without preceding session ID at line %d: \"%s\"", lineNumber, line)
                    );
                }
                String path = StringUtil.stripLeading( line );
                if (!ZNODE_PATH_PATTERN_OF_WCHC.matcher(path).matches()) {
                    throw new IllegalArgumentException(
                            String.format("Invalid znode path at line %d: \"%s\"", lineNumber, path)
                    );
                }
                result.get(currentSessionId).add(path);
            }
        }

        return result;
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

