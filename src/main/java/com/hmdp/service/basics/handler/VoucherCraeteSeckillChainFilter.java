package com.hmdp.service.basics.handler;

import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.service.basics.chain.MerchantAdminAbstractChainHandler;
import com.hmdp.utils.DiscountTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

import static com.hmdp.utils.ChainBizMarkEnum.MERCHANT_ADMIN_CRETE_VOUCHER_KEY;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 16:25
 * @Description: 优惠券类型判断，秒杀优惠券创建
 */
@Component
public class VoucherCraeteSeckillChainFilter implements MerchantAdminAbstractChainHandler<VoucherSaveReqDTO> {

    private final int maxStock = 20000000;

    @Override
    public void handler(VoucherSaveReqDTO requestParam) {
        // 优惠券类型校验
        boolean typeAnyMatch = Arrays.stream(DiscountTypeEnum.values())
                .anyMatch(enumConstant -> enumConstant.getType() == requestParam.getType());
        if (!typeAnyMatch) {
            throw new RuntimeException("优惠券类型不存在");
        }

        // 秒杀优惠券参数校验
        if (requestParam.getType() == DiscountTypeEnum.SECKILL_VOUCHER.getType()) {
            if (requestParam.getBeginTime().isAfter(requestParam.getEndTime())) {
                throw new RuntimeException("有效期范围错误");
            }

            LocalDateTime now = LocalDateTime.now();
            if (requestParam.getEndTime().isBefore(now)) {
                throw new RuntimeException("有效期结束时间早于当前");
            }

            if (requestParam.getStock() <= 0 || requestParam.getStock() > maxStock) {
                throw new RuntimeException("库存数量设置异常");
            }
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CRETE_VOUCHER_KEY.name();
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
