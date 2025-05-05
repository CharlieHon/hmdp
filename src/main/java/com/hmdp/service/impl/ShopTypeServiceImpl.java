package com.hmdp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result listShopType() {
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);
        if (CollUtil.isNotEmpty(shopTypeList)) {
            // 缓存中存在商铺类型
            List<ShopType> shopTypes = shopTypeList.stream()
                    .map(jsonStr -> JSONUtil.toBean(jsonStr, ShopType.class))
                    .sorted(Comparator.comparing(ShopType::getSort))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }
        // 缓存中不存在
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if (CollUtil.isEmpty(shopTypes)) {
            return Result.fail("店铺类型访问异常！");
        }
        List<String> jsonStrList = shopTypes.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, jsonStrList);
        return Result.ok(shopTypes);
    }
}
