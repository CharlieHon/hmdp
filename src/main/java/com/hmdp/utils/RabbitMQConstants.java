package com.hmdp.utils;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/26 21:04
 * @Description: 消息队列常量
 */
public class RabbitMQConstants {
    public static final String SECKILL_ORDER_EXCHANGE = "hmdq.seckill.direct";

    public static final String SECKILL_ORDER_QUEUE = "seckill.success.queue";

    public static final String SECKILL_ORDER_SUCCESS_KEY = "seckill.success";
}
