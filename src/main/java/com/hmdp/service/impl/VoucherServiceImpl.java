package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.req.VoucherSaveReqDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.service.basics.chain.MerchantAdminChainContext;
import com.hmdp.utils.DiscountTypeEnum;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.hmdp.utils.ChainBizMarkEnum.MERCHANT_ADMIN_CRETE_VOUCHER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    private final ISeckillVoucherService seckillVoucherService;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 责任链上下文
     */
    private final MerchantAdminChainContext<VoucherSaveReqDTO> merchantAdminChainContext;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 查询优惠券信息
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        // 返回结果
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 保存优惠券
        save(voucher);
        // 保存秒杀信息
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        // 保存秒杀库存到redis中
        // set seckill:stock:voucher_id stock
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
    }

    /**
     * 创建优惠券
     *
     * @param requestParam 请求参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucher(VoucherSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CRETE_VOUCHER_KEY.toString(), requestParam);

        // 新增优惠券信息到数据库
        Voucher voucherDO = BeanUtil.toBean(requestParam, Voucher.class);
        voucherDO.setStatus(1);
        voucherDO.setShopId(UserHolder.getUser().getShopId());
        // 保存优惠券
        save(voucherDO);

        if (!Objects.equals(requestParam.getType(), DiscountTypeEnum.SECKILL_VOUCHER.getType())) {
            // 非秒杀优惠券
            return;
        }

        SeckillVoucher seckillVoucherDO = BeanUtil.toBean(requestParam, SeckillVoucher.class);
        seckillVoucherDO.setVoucherId(voucherDO.getId());
        // 保存秒杀优惠券
        seckillVoucherService.save(seckillVoucherDO);

        // 秒杀优惠券，缓存预热   seckill:stock:voucher_id -> stock
        String seckillVoucherKey = SECKILL_STOCK_KEY + seckillVoucherDO.getVoucherId();

        String luaScript = "redis.call('set', KEYS[1], ARGV[1]) " +
                "redis.call('EXPIREAT', KEYS[1], ARGV[2])";

        List<String> keys = Collections.singletonList(seckillVoucherKey);
        List<String> args = new ArrayList<>();
        args.add(seckillVoucherDO.getStock().toString());
        args.add(String.valueOf(seckillVoucherDO.getEndTime().toEpochSecond(ZoneOffset.UTC)));

        stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                keys,
                args.toArray()
        );
    }
}
