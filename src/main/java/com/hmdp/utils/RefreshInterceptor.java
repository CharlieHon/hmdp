package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Author: charlie
 * @CreateTime: Created in 2025/2/23 18:44
 * @Description: 登录状态刷新拦截器
 * 1. 由于拦截器是手动创建的，spring不会管理其生命周期以及依赖注入。需要通过构造器注入属性
 * 2. 拦截所有请求，刷新token有效期
 */
@RequiredArgsConstructor
public class RefreshInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取登录token
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // 没有携带 authorization 请求头，说明还没有登录，直接放行。交给 LoginInterceptor 拦截器处理
            return true;
        }
        // 2. 基于Token获取redis中的用户
        // login:token:UUID -> hash
        String tokenKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(tokenKey);
        // 3. 判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        // 5. 将查询到的Hash数据转为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6. 存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        if (userDTO.getId() != 1010) {
            // 7. 刷新token有效期
            stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
        // 8. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
