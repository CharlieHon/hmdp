package com.hmdp.config;

import com.hmdp.idempotent.NoDuplicateSubmitAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 21:15
 * @Description: 幂等组件相关配置类
 */
@Configuration
public class IdempotentConfiguration {

    // /**
    //  * 防止用户重复提交表单信息切面控制器
    //  * 注意：该方法使用 redisson 分布式锁，经实际测试可能是由于创建优惠券速度很快，并没有阻塞住压测进来的多个请求
    //  * 测试：10个请求失败4，50*2个请求，失败55
    //  */
    // @Bean
    // @Deprecated
    // public NoDuplicateSubmitAspect noDuplicateSubmitAspectWithRedisson(RedissonClient redissonClient) {
    //     return new NoDuplicateSubmitAspect(redissonClient);
    // }

    /**
     * 使用 setnx key value ex 2 进制2秒内相同请求参数的重复提交
     * 测试：5*2只有一个创建请求成功
     */
    @Bean
    public NoDuplicateSubmitAspect noDuplicateSubmitAspect(StringRedisTemplate stringRedisTemplate) {
        return new NoDuplicateSubmitAspect(stringRedisTemplate);
    }
}
