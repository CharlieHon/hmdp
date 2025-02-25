package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;

@SpringBootTest
class UserLoginTests {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testGenerateUser() {
        long phone = 17600000000L;
        for (int i = 0; i < 1000; i++) {
            User user = new User();
            user.setPhone(String.valueOf(phone));
            phone++;
            // 生成随机昵称
            user.setNickName("user_" + RandomUtil.randomString(10));
            userMapper.insert(user);
        }
    }

    @Test
    void testLogin() throws IOException {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.gt(true, User::getId, 1005L);
        List<User> users = userMapper.selectList(userLambdaQueryWrapper);
        File tokens = new File("D:\\you_dir\\tokens.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(tokens);
        for (User user : users) {
            String token = UUID.randomUUID().toString(true);
            fileOutputStream.write(token.getBytes());
            fileOutputStream.write("\r\n".getBytes());
            // userDTO转map
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>()
                    , CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor(
                                    (name, value) -> value.toString()
                            ));
            // 保存用户信息到redis
            stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            // 设置过期时间
            // stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
    }
}
