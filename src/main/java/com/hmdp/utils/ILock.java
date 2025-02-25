package com.hmdp.utils;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/25 16:29
 * @Description: Redis分布式锁接口
 */
public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 锁过期时间，单位秒
     * @return 成功返回true，失败返回false
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
