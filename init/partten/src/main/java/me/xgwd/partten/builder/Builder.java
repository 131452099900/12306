package me.xgwd.partten.builder;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/15/15:33
 * @Description:
 */

import java.io.Serializable;

/**
 * Builder 模式抽象接口
 */
public interface Builder<T> extends Serializable {

    /**
     * 构建方法
     *
     * @return 构建后的对象
     */
    T build();
}
