package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.entity.Voucher;
import com.hmdp.idempotent.NoDuplicateSubmit;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增优惠券
     * @param requestParam 请求入参
     * @return
     */
    @PostMapping
    @NoDuplicateSubmit(message = "您操作过快，请稍后重试[createVoucher]")
    public Result addVoucher(@RequestBody VoucherSaveReqDTO requestParam) {
        voucherService.createVoucher(requestParam);
        return Result.ok();
    }

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("/seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
