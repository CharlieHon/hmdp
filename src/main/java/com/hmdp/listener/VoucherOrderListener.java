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

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hmdp.utils.RabbitMQConstants.*;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/26 20:25
 * @Description: 数据库创建订单消费者
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherOrderListener {

    private final VoucherOrderServiceImpl voucherOrderService;

    private final RedissonClient redissonClient;

    private static final AtomicInteger threadCount = new AtomicInteger();

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() + 1,
            0,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
                Thread t = new Thread(r);
                t.setName("createVoucherOrderThread-" + threadCount.incrementAndGet());
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

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
