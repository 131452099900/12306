package me.xgwd.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author:
 * @Date: 2024/09/14/10:57
 * @Description:
 */
@RequiredArgsConstructor
public class RedisKeySerializer implements InitializingBean, RedisSerializer<String> {
    private final String keyPrefix;

    private final String charsetName;

    private Charset charset;
    @Override
    public void afterPropertiesSet() throws Exception {
        // bean初始时回调 根据charsetName解析charset
        charset = Charset.forName(charsetName);
    }

    @Override
    public byte[] serialize(String s) throws SerializationException {
        String realKey = keyPrefix.concat(keyPrefix);
        return keyPrefix.getBytes(charset);
    }

    @Override
    public String deserialize(byte[] bytes) throws SerializationException {
        return new String(bytes, charset);
    }
}
