package me.xgwd.base;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/14/10:09
 * @Description:
 */
public class ApplicationContextHolder implements ApplicationContextAware {

    // 通过SPI机制 在容器初始化后赋值
    private static ApplicationContext CONTEXT;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.CONTEXT = applicationContext;
    }

    /**
     * Get ioc container bean by type.
     */
    public static <T> T getBean(Class<T> clazz) {
        return CONTEXT.getBean(clazz);
    }

    /**
     * Get ioc container bean by name.
     */
    public static Object getBean(String name) {
        return CONTEXT.getBean(name);
    }

    /**
     * Get ioc container bean by name and type.
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return CONTEXT.getBean(name, clazz);
    }

    /**
     * Get a set of ioc container beans by type.
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return CONTEXT.getBeansOfType(clazz);
    }

    /**
     * Find whether the bean has annotations.
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        return CONTEXT.findAnnotationOnBean(beanName, annotationType);
    }

    /**
     * Get application context.
     */
    public static ApplicationContext getInstance() {
        return CONTEXT;
    }
}

