package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/25 20:35
 * @Description: RedissonConfig
 * Redisson需要显式的配置类，因为Redisson支持多种连接模式（单节点、主从等），没有合理的默认值
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 配置
        Config config = new Config();
        config.useSingleServer()
                .setDatabase(1)     // index 1 for hmdp_orz
                .setAddress("redis://192.168.226.133:6379")
                .setPassword("12345");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }

}
