package com.hmdp.utils;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 15:54
 * @Description: 定义业务责任链类型枚举
 */
public enum ChainBizMarkEnum {

    /**
     * 创建优惠券验证参数是否正确责任链流程
     */
    MERCHANT_ADMIN_CRETE_VOUCHER_KEY;

    @Override
    public String toString() {
        return this.name();
    }
}
