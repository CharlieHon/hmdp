package com.hmdp.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 16:29
 * @Description: 优惠券类型
 */
@Getter
@RequiredArgsConstructor
public enum DiscountTypeEnum {

    COMMON_VOUCHER(0, "普通券"),

    SECKILL_VOUCHER(1, "秒杀券");


    private final int type;

    private final String value;

    public static String findValueByType(int type) {
        for (DiscountTypeEnum target : DiscountTypeEnum.values()) {
            if (target.getType() == type) {
                return target.getValue();
            }
        }
        throw new IllegalArgumentException();
    }
}
