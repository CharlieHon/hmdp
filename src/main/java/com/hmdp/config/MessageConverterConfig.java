package com.hmdp.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/26 20:22
 * @Description: 消息队列消息转换配置类
 */
@Configuration
public class MessageConverterConfig {

    @Bean
    public MessageConverter messageConverter(){
        // 1. 定义消息转换器
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // 2. 配置自动创建消息id，用于识别不同消息，也可以在业务中基于ID判断消息是否重复
        converter.setCreateMessageIds(true);
        return converter;
    }

}
