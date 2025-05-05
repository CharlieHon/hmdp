package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    /**
     * 根据店铺id查询店铺信息
     * @param id 店铺id
     * @return 店铺信息
     */
    Result queryShopById(Long id);

    Result updateShop(Shop shop);
}
