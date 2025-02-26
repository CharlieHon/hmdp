package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀优惠卷下单
     * @param voucherId 优惠卷id
     * @return 返回优惠卷订单id
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 创建优惠卷订单
     * @param voucherId 优惠卷id
     * @return
     */
    Result createVoucherOrder(Long voucherId);

    /**
     * 基于阻塞队列的异步下单
     * @param voucherOrder 优惠券订单
     */
    void createVoucherOrder(VoucherOrder voucherOrder);
}
