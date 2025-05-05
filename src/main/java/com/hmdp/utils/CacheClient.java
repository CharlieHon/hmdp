package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/24 19:34
 * @Description: 封装redis工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    private static final AtomicInteger i = new AtomicInteger();
    // 使用线程池获取线程
    // private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = new ThreadPoolExecutor(
            4,
            4,
            0,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("CACHE_REBUILD_THREAD_" + i.incrementAndGet());
                return t;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    /**
     * 将任意Java对象序列化为json并存储在string类型得key中，并且设置ttl过期时间
     *
     * @param key   redis键名
     * @param value java序列化json字符串
     * @param time  过期时间
     * @param unit  过期时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 将任意Java对象序列化为json并存储在string类型的key中，并且可以设置逻辑过期时间，用于处理缓存击穿问题
     *
     * @param key   redis键名
     * @param value java序列化json字符串
     * @param time  过期时间
     * @param unit  过期时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，利用缓存空值的方式解决缓存穿透问题
     *
     * @param keyPrefix  redis键前缀
     * @param id         数据id
     * @param type       数据类型
     * @param dbFallback 查询数据库相关逻辑
     * @param time       过期时间
     * @param unit       过期时间单位
     * @param <R>        数据类型
     * @param <ID>       数据id类型
     * @return
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }

        // 判断命中得是否是空值
        if (json != null) { // 不是null，则就是缓存的空字符串 ""
            // 返回一个错误信息
            return null;
        }

        // json == null 说明没有建立缓存，去数据库查询。如果数据库中不存在，则说明缓存穿透，缓存空对象
        R r = dbFallback.apply(id);
        if (r == null) {
            // 缓存穿透：缓存空对象，并设置一个较短的有效期
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 存在，写入redis
        set(key, r, time, unit);
        return r;
    }

    /**
     * 根据指定的key查询缓存，并反序列化为指定类型，需要利用逻辑过期解决缓存击穿问题
     *
     * @param keyPrefix  redis键前缀
     * @param id         数据id
     * @param type       数据类型
     * @param dbFallback 查询数据库相关逻辑
     * @param time       过期时间
     * @param unit       过期时间单位
     * @param lockPrefix 锁前缀
     * @param <R>        数据类型
     * @param <ID>       数据id类型
     * @return
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit, String lockPrefix) {
        String key = keyPrefix + id;
        // 1. 从redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断是否存在
        if (StrUtil.isBlank(json)) {
            // 3. 不存在，直接返回（缓存穿透）
            return null;
        }
        // 4. 命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5. 判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 5.1 未过期，直接返回店铺信息
            return r;
        }

        // 5.2 已过期，需要缓存重建
        // 6. 缓存重建
        // 6.1 获取互斥锁
        String lockKey = lockPrefix + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {

            // 6.2 DoubleCheck
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isBlank(json)) {
                return null;
            }
            redisData = JSONUtil.toBean(json, RedisData.class);
            r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
            expireTime = redisData.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())) {
                return r;
            }

            // 6.3 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    // 查询数据库
                    R r1 = dbFallback.apply(id);
                    // 重建缓存
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4 返回过期的信息
        return r;
    }

    /**
     * 缓存击穿——互斥锁
     * @param keyPrefix 前缀
     * @param id 数据id
     * @param type 数据类型
     * @param dbCallback 数据库回调函数
     * @param timeout 过期时间
     * @param unit  过期时间单位
     * @return
     * @param <R> 返回类型
     * @param <ID> ID类型
     */
    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbCallback, Long timeout, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1. 从 redis 中查询商铺缓存
        String jsonStr = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断缓存是否存在
        if (StrUtil.isNotBlank(jsonStr)) {
            // 3. 存在，直接返回
            return JSONUtil.toBean(jsonStr, type);
        }

        // 是缓存的空指 ""
        if (jsonStr != null) {
            // 返回 null 值
            return null;
        }

        // 4. 不存在，实现缓存重建
        // 4.1 获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean flag = tryLock(lockKey);
            // 4.2 判断锁是否获取成功
            if (!flag) {
                // 4.3 获取锁失败，休眠并重试
                Thread.sleep(50);
                return queryWithMutex(key, id, type, dbCallback, timeout, unit);
            }

            // 4.4 获取锁成功，双重检验
            jsonStr = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(jsonStr)) {
                return JSONUtil.toBean(jsonStr, type);
            }
            if (jsonStr != null) {
                return null;
            }

            // 4.5 查询数据库重建缓存
            r = dbCallback.apply(id);
            // 5. 数据库中不存在，缓存穿透
            if (r == null) {
                // 缓存穿透：缓存空对象
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            // 6. 存在，重建缓存
            this.set(key, r, timeout, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7. 释放锁
            unlock(lockKey);
        }
        // 8. 返回
        return r;
    }

    /**
     * 获取互斥锁 setnx key "1"
     *
     * @param key redis键名
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 释放互斥所 del key
     *
     * @param key redis键名
     */
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
