package com.hmdp.idempotent;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson2.JSON;
import com.hmdp.utils.UserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 21:00
 * @Description: 防止用户重复提交表单信息切面控制器
 */
@Aspect
public class NoDuplicateSubmitAspect {

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(com.hmdp.idempotent.NoDuplicateSubmit)")
    public Object noDuplicateSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        NoDuplicateSubmit noDuplicateSubmit = getNoDuplicateSubmitAnnotation(joinPoint);
        // 获取分布式锁标识
        String lockKey = String.format("no-duplicate-submit:path:%s:userId:%s:md:%s", getServletPath(), getCurrentUserId(), calculateArgsMD5(joinPoint));
        Boolean lockSuccess = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "-", 2, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(lockSuccess)) {
            throw new RuntimeException(noDuplicateSubmit.message());
        }
        // 执行标记了防重复提交注解的方法原逻辑
        return joinPoint.proceed();
    }

    // /**
    //  * 增强方法标记 {@link NoDuplicateSubmit} 逻辑注解
    //  */
    // @Deprecated
    // @Around("@annotation(com.hmdp.idempotent.NoDuplicateSubmit)")
    // public Object noDuplicateSubmitWithRedisson(ProceedingJoinPoint joinPoint) throws Throwable {
    //     NoDuplicateSubmit noDuplicateSubmit = getNoDuplicateSubmitAnnotation(joinPoint);
    //     // 获取分布式锁标识
    //     String lockKey = String.format("no-duplicate-submit:path:%s:userId:%s:md:%s", getServletPath(), getCurrentUserId(), calculateArgsMD5(joinPoint));
    //     System.out.println(lockKey);
    //     RLock lock = redissonClient.getLock(lockKey);
    //     // 尝试获取锁，获取锁失败就意味着已经重复提交，直接抛出异常
    //     if (!lock.tryLock()) {
    //         throw new RuntimeException(noDuplicateSubmit.message());
    //     }
    //
    //     Object result;
    //     try {
    //         // 执行标记了防重复提交注解的方法原逻辑
    //         result = joinPoint.proceed();
    //     } finally {
    //         lock.unlock();
    //     }
    //     return result;
    // }

    /**
     * 返回自定义防重复提交注解
     */
    public static NoDuplicateSubmit getNoDuplicateSubmitAnnotation(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(signature.getName(), signature.getMethod().getParameterTypes());
        return targetMethod.getAnnotation(NoDuplicateSubmit.class);
    }

    /**
     * @return 获取当前线程上下文请求路径 ServletPath
     */
    private String getServletPath() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest().getServletPath();
    }

    /**
     * @return 当前操作用户ID
     */
    private String getCurrentUserId() {
        return UserHolder.getUser().getId().toString();
    }

    /**
     * 根据请求参数生成唯一标识，相同标识意味着请求参数相同
     */
    private String calculateArgsMD5(ProceedingJoinPoint joinPoint) {
        return DigestUtil.md5Hex(JSON.toJSONBytes(joinPoint.getArgs()));
    }

    public NoDuplicateSubmitAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = null;
    }

    public NoDuplicateSubmitAspect(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = null;
    }
}
