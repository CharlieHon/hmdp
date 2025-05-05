package com.hmdp.service.basics.handler;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.service.basics.chain.MerchantAdminAbstractChainHandler;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.hmdp.utils.ChainBizMarkEnum.MERCHANT_ADMIN_CRETE_VOUCHER_KEY;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 16:12
 * @Description: 验证优惠券创建接口参数是否正确责任链
 */
@Component
public class VoucherCreateParamNotNullChainFilter implements MerchantAdminAbstractChainHandler<VoucherSaveReqDTO> {

    @Override
    public void handler(VoucherSaveReqDTO requestParam) {
        if (StrUtil.isEmpty(requestParam.getTitle())) {
            throw new RuntimeException("优惠券名称不能为空");
        }

        if (StrUtil.isEmpty(requestParam.getRules())) {
            throw new RuntimeException("优惠券使用规则不能为空");
        }

        if (Objects.isNull(requestParam.getPayValue())) {
            throw new RuntimeException("优惠券消费金额不能为空");
        }

        if (Objects.isNull(requestParam.getActualValue())) {
            throw new RuntimeException("优惠券扣减金额不能为空");
        }

        if (Objects.isNull(requestParam.getType())) {
            throw new RuntimeException("优惠券扣减金额不能为空");
        }
    }

    @Override
    public String mark() {
        return MERCHANT_ADMIN_CRETE_VOUCHER_KEY.name();
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
