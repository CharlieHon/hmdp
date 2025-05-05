package com.hmdp.listener;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.hmdp.utils.RabbitMQConstants.*;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/26 20:25
 * @Description: VoucherOrderListener
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherOrderListener {

    private final VoucherOrderServiceImpl voucherOrderService;

    private final RedissonClient redissonClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = SECKILL_ORDER_QUEUE, durable = "true"),
            exchange = @Exchange(name = SECKILL_ORDER_EXCHANGE, type = ExchangeTypes.DIRECT),
            key = {SECKILL_ORDER_SUCCESS_KEY}
    ))
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        // 都是一个线程 thread:org.springframework.amqp.rabbit.RabbitListenerEndpointContainer#0-1
        // log.debug("thread:" + Thread.currentThread().getName());
        RLock lock = redissonClient.getLock("lock:voucher-order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("amqp:一人只能下一单");
            return;
        }

        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
        log.debug("用户{}下单成功", userId);
    }
}
