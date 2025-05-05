package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.entity.Voucher;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherService extends IService<Voucher> {

    Result queryVoucherOfShop(Long shopId);

    void addSeckillVoucher(Voucher voucher);

    void createVoucher(VoucherSaveReqDTO requestParam);
}
