package me.xgwd.api.common;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/21/11:59
 * @Description:
 */

import lombok.extern.slf4j.Slf4j;
import me.xgwd.base.exception.AbstractException;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.filter.ExceptionFilter;


/**
 * 重写Dubbo内部exception
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class DubboExceptionFilter extends ExceptionFilter {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return super.invoke(invoker, invocation);
    }


    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        Throwable exception = appResponse.getException();
        if (exception instanceof AbstractException) return;
        super.onResponse(appResponse, invoker, invocation);
    }
}


