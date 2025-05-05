package com.hmdp.dto.req;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 15:56
 * @Description: 优惠券模板新增接口请求参数实体
 */
@Data
public class VoucherSaveReqDTO {

    // /**
    //  * 优惠券对应的店铺ID
    //  */
    // private Long shopId;

    /**
     * 优惠券标题
     */
    private String title;

    /**
     * 优惠券副标题
     */
    private String subTitle;

    /**
     * 优惠券使用规则
     */
    private String rules;

    /**
     * 优惠券支付金额
     */
    private Long payValue;

    /**
     * 优惠券扣减后金额
     */
    private Long actualValue;

    /**
     * 优惠券类型 0：普通券，1：秒杀券
     */
    private Integer type;

    /**
     * 优惠券状态 1：上架，2下架，3过期
     */
    private Integer status;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 开始时间
     */
    private LocalDateTime beginTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;
}
