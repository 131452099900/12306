package me.xgwd.api.common;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.filter.ConsumerContextFilter;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/21/15:32
 * @Description:
 */
@Activate(order = 100,group = {CommonConstants.CONSUMER})
public class Qk extends ConsumerContextFilter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        System.out.println("哈哈哈哈我是消费者qwdasdasdasdasd");
        return super.invoke(invoker, invocation);
    }
}
