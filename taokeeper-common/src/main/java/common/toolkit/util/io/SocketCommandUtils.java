package common.toolkit.util.io;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import common.toolkit.entity.HostPerformanceEntity;
import common.toolkit.entity.io.SSHResource;
import common.toolkit.exception.IllegalParamException;
import common.toolkit.exception.SSHException;
import common.toolkit.util.StringUtil;
import common.toolkit.util.number.IntegerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static common.toolkit.constant.BaseConstant.WORD_SEPARATOR;
import static common.toolkit.constant.EmptyObjectConstant.EMPTY_STRING;
import static common.toolkit.constant.HtmlTagConstant.BR;
import static common.toolkit.constant.SymbolConstant.COMMA;
import static common.toolkit.constant.SymbolConstant.PERCENT;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author  nileader / nileader@gmail.com
 * @Date	 2018-06-21
 */
public class SocketCommandUtils {

    private static final int DEFAULT_TIMEOUT_MS = 5000; // 默认5秒超时

    /**
     * 向指定 IP 和端口发送命令，并将完整响应作为字符串返回。
     *
     * @param ip       目标服务器 IP 或主机名
     * @param port     目标端口
     * @param command  要发送的命令（如 "stat"）
     * @return         完整响应内容（UTF-8 解码）
     * @throws IOException 网络或IO异常
     */
    public static String executeSocketCommand(String ip, int port, String command) throws IOException {
        return executeSocketCommand(ip, port, command, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 带超时控制的字符串版本
     */
    public static String executeSocketCommand(String ip, int port, String command, int timeoutMs) throws IOException {
        StringBuilder response = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = executeSocketCommandAsStream(ip, port, command, timeoutMs);
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append(System.lineSeparator());
            }
        }finally {
            if(null!=inputStream) inputStream.close();
            if(null!=reader) reader.close();
        }
        return response.toString().trim();
    }

    /**
     * 向指定 IP 和端口发送命令，并返回响应的 InputStream。
     * 调用者负责关闭该流（建议使用 try-with-resources）。
     *
     * @param ip       目标服务器 IP 或主机名
     * @param port     目标端口
     * @param command  要发送的命令（如 "stat"）
     * @return         响应的 InputStream（需手动关闭）
     * @throws IOException 网络或IO异常
     */
    public static InputStream executeSocketCommandAsStream(String ip, int port, String command) throws IOException {
        return executeSocketCommandAsStream(ip, port, command, DEFAULT_TIMEOUT_MS);
    }

    /**
     * 带超时控制的流版本
     */
    public static InputStream executeSocketCommandAsStream(String ip, int port, String command, int timeoutMs) throws IOException {
        if (ip == null || ip.isEmpty()) {
            throw new IllegalArgumentException("IP/Host cannot be null or empty");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }
        if (command.getBytes(StandardCharsets.US_ASCII).length != command.length()) {
            throw new IllegalArgumentException("Command must be ASCII-only (required by four-letter commands)");
        }

        Socket socket = new Socket();
        try {
            socket.connect(new java.net.InetSocketAddress(ip, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            // 发送命令（必须是原始字节，且为4字节对齐，但这里不限制长度以支持通用场景）
            OutputStream out = socket.getOutputStream();
            out.write(command.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            // 返回输入流（由调用者负责读取和关闭）
            return new FilterInputStream(socket.getInputStream()) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        socket.close();
                    }
                }
            };

        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }
}
