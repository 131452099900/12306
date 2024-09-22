package me.xgwd.api.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.trace.TraceUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/21/12:05
 * @Description:
 */
@Slf4j
@Activate(group = CONSUMER, order = -10000)
public class DubboTraceFilter implements Filter{
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 处理 Trace 信息
        printRequest(invocation);
        // 执行前
        handleTraceId();
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);
        long end = System.currentTimeMillis();
        // 执行后
        printResponse(invocation,result,end-start);
        return result;
    }

    /***
     *  打印请求
     * @author winfun
     * @param invocation invocation
     * @return {@link }
     **/
    private void printRequest(Invocation invocation){
        Map<String, Object> map = new HashMap<>();
        map.put("InterfaceClassNam", invocation.getInvoker().getInterface().getName());
        map.put("MethodName", invocation.getMethodName());
        map.put("Args", getArgs(invocation));
        log.info("call Dubbo Api start , request is {}",map);
    }

    /***
     *  打印结果
     * @author winfun
     * @param invocation invocation
     * @param result result
     * @return {@link }
     **/
    private void printResponse(Invocation invocation,Result result,long spendTime){
        Map<String, Object> map = new HashMap<>();
        map.put("InterfaceClassNam", invocation.getInvoker().getInterface().getName());
        map.put("MethodName", invocation.getMethodName());
        map.put("Result", JSON.toJSONString(result.getValue()));
        map.put("SpendTime", spendTime);
        log.info("call Dubbo Api end , response is {}",map);
    }

    /***
     *  获取 Invocation 参数，过滤掉大参数
     * @author winfun
     * @param invocation invocation
     * @return {@link Object[] }
     **/
    private Object[] getArgs(Invocation invocation){
        Object[] args = invocation.getArguments();
        args = Arrays.stream(args).filter(arg->{
            if (arg instanceof byte[] || arg instanceof Byte[] || arg instanceof InputStream || arg instanceof File){
                return false;
            }
            return true;
        }).toArray();
        return args;
    }

    /***
     *  处理 TraceId，如果当前对象是服务消费者，则将 Trace 信息放入 RpcContext中
     *  如果当前对象是服务提供者，则从 RpcContext 中获取 Trace 信息。
     * @author winfun

     * @return {@link  }
     **/
    private void handleTraceId() {
        RpcContext context = RpcContext.getContext();
        if (context.isConsumerSide()) {
            TraceUtil.putTraceInto(context);
        } else if (context.isProviderSide()) {
            TraceUtil.getTraceFrom(context);
        }
    }


}

