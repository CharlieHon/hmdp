package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static com.hmdp.utils.RabbitMQConstants.SECKILL_ORDER_EXCHANGE;
import static com.hmdp.utils.RabbitMQConstants.SECKILL_ORDER_SUCCESS_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private final ISeckillVoucherService seckillVoucherService;

    private final RedisIdWorker redisIdWorker;

    /**
     * 向消息队列发送消息
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 自定义redis分布式锁
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * redisson
     */
    private final RedissonClient redissonClient;

    /**
     * 执行lua脚本
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 1. 执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        // 2. 判断结果是否为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1 不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足~" : "不能重复下单");
        }

        // 2.2 为0，有购买资格，把下单信息保存到阻塞队列
        // 保存下单信息到阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3 订单id
        long orderId = redisIdWorker.nextId("voucher-order");
        voucherOrder.setId(orderId);
        // 2.4 用户id
        voucherOrder.setUserId(userId);
        // 2.5 代金券id
        voucherOrder.setVoucherId(voucherId);

        // 基于rabbitmq消息队列实现秒杀异步下单 exchange routingKey queue
        // hmdq.seckill.direct      seckill.success
        rabbitTemplate.convertAndSend(
                SECKILL_ORDER_EXCHANGE,
                SECKILL_ORDER_SUCCESS_KEY,
                voucherOrder);

        // 3. 返回订单id
        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 在数据库层面再次进行兜底判断，防止库存超卖和一人多单问题
        // 5. 一人一单
        Long userId = UserHolder.getUser().getId();
        // 5.1 查询订单
        int count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        // 5.2 判断是否存在
        if (count > 0) {
            // 用户已经购买过
            return Result.fail("用户已经购买过一次");
        }

        // 6. 扣减库存 update table set stock = stock - 1 where voucher_id = ? and stock > 0
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            // 扣减失败
            return Result.fail("库存不足");
        }

        // 6. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 6.1 订单id
        long orderId = redisIdWorker.nextId("voucher-order");
        voucherOrder.setId(orderId);
        // 6.2 用户id
        voucherOrder.setUserId(userId);
        // 6.3 代金券id
        voucherOrder.setVoucherId(voucherId);
        // 6.4 保存订单
        save(voucherOrder);
        // 7. 返回订单id
        return Result.ok(orderId);
    }

    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 5.1 查询订单
        int count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        // 5.2 判断是否存在
        if (count > 0) {
            log.error("用户已经购买过一单");
            return;
        }
        // 6. 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            // 扣减失败
            log.error("订单库存不足");
            return;
        }
        // 6.4 保存订单
        save(voucherOrder);
    }
}
