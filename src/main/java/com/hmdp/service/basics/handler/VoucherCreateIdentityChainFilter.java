package com.hmdp.service.basics.handler;

import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.service.basics.chain.MerchantAdminAbstractChainHandler;

import static com.hmdp.utils.ChainBizMarkEnum.MERCHANT_ADMIN_CRETE_VOUCHER_KEY;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 16:17
 * @Description: 创建优惠券用户身份校验
 */
// @Component
@Deprecated
public class VoucherCreateIdentityChainFilter implements MerchantAdminAbstractChainHandler<VoucherSaveReqDTO> {

    @Override
    public void handler(VoucherSaveReqDTO requestParam) {
        // 不在责任链中校验权限
        // if (!Objects.equals(requestParam.getShopId(), UserHolder.getUser().getShopId())) {
        //     throw new RuntimeException("创建优惠券错误，请创建本店铺商品优惠券");
        // }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CRETE_VOUCHER_KEY.name();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
