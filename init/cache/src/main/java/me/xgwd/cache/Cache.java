package me.xgwd.cache;


import javax.validation.constraints.NotBlank;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/14/10:35
 * @Description:
 */
public interface Cache {
    /**
     * 获取缓存
     */
    <T> T get(@NotBlank String key, Class<T> clazz);

    /**
     * 放入缓存
     */
    void put(@NotBlank String key, Object value);
}
