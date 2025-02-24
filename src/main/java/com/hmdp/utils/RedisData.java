package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Redis存储数据对象，包含逻辑过期时间
 * 存储数据类型为Object，使用JSONUtil.toBean反序列化时，会反序列化为JSONObject类型
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
