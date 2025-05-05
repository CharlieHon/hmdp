package com.hmdp.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/5/5 20:57
 * @Description: 自定义防重复提交注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoDuplicateSubmit {

    /**
     * 触发幂等失败逻辑时，返回错误提示消息
     */
    String message() default "您操作过快，请稍后重试";
}
