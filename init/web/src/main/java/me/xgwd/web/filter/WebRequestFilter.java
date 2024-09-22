package me.xgwd.web.filter;

import lombok.extern.slf4j.Slf4j;
import me.xgwd.api.trace.TraceUtil;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/21/12:52
 * @Description:
 */
@Slf4j
@Order(1)
@WebFilter(urlPatterns = "/*")
public class WebRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException, ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String uri = request.getRequestURI();
        // 初始化 TraceId
        TraceUtil.initTrace(uri);
        filterChain.doFilter(request,response);

        // 清除 TraceId 和 TraceUri
        TraceUtil.clearTrace();
    }

}
