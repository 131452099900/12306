package me.xgwd.api.trace;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/21/12:46
 * @Description:
 */

import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Trace 工具
 * @author winfun
 * @date 2020/10/30 9:02 上午
 **/
public class TraceUtil {

    public final static String TRACE_ID = "trace_id";
    public final static String TRACE_URI = "uri";

    /**
     * 初始化 TraceId
     * @param uri 请求uri
     */
    public static void initTrace(String uri) {
        if(StringUtils.isEmpty(MDC.get(TRACE_ID))) {
            String traceId = generateTraceId();
            setTraceId(traceId);
            MDC.put(TRACE_URI, uri);
            System.out.println("消费者已经设置成功进MDC啦" +MDC.get(TRACE_ID));
        }
    }

    /**
     * 从 RpcContext 中获取 Trace 相关信息，包括 TraceId 和 TraceUri
     * 给 Dubbo 服务端调用
     * @param context Dubbo 的 RPC 上下文
     */
    public static void getTraceFrom(RpcContext context) {
        String traceId = context.getAttachment(TRACE_ID);
        if (StringUtils.hasText(traceId)){
            setTraceId(traceId);
        }
        String uri = context.getAttachment(TRACE_URI);
        if (StringUtils.hasText(uri)) {
            MDC.put(TRACE_URI, uri);
        }
    }

    /**
     * 将 Trace 相关信息，包括 TraceId 和 TraceUri 放入 RPC上下文中
     * 给 Dubbo 消费端调用
     * @param context Dubbo 的 RPC 上下文
     */
    public static void putTraceInto(RpcContext context) {
        String traceId = MDC.get(TRACE_ID);
        if (StringUtils.hasText(traceId)) {
            context.setAttachment(TRACE_ID, traceId);
        }

        String uri = MDC.get(TRACE_URI);
        if (StringUtils.hasText(uri)) {
            context.setAttachment(TRACE_URI, uri);
        }
    }

    /**
     * 从 MDC 中清除当前线程的 Trace信息
     */
    public static void clearTrace() {
        MDC.clear();
    }

    /**
     * 将traceId放入MDC
     * @param traceId   链路ID
     */
    private static void setTraceId(String traceId) {
        traceId = left(traceId, 36);
        MDC.put(TRACE_ID, traceId);
    }

    public static String left(String str, int len) {
        if (str == null) {
            return null;
        } else if (len < 0) {
            return "";
        } else {
            return str.length() <= len ? str : str.substring(0, len);
        }
    }

    /**
     * 生成traceId
     * @return  链路ID
     */
    private static String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}

