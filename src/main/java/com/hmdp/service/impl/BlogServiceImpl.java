package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@RequiredArgsConstructor
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private final IUserService userService;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            // 查询发笔记的用户信息
            this.queryBlogUser(blog);
            // 判断笔记是否被当前用户点赞
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryBlogById(Long id) {
        // 1. 查询blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        // 2. 查询blog有关的用户
        queryBlogUser(blog);
        // 3. 查询blog是否被点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    /**
     * 查询笔记是否被当前用户顶赞
     * @param blog 笔记
     */
    private void isBlogLiked(Blog blog) {
        Long userId = UserHolder.getUser().getId();
        String key = "blog:liked:" + blog.getId();
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        blog.setIsLike(Boolean.TRUE.equals(isMember));
    }

    /**
     * 填充笔记用户信息
     * @param blog 笔记
     */
    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }

    @Override
    public Result likeBlog(Long id) {
        // 1. 判断当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 用户是否已经点赞
        String key = "blog:liked:" + id;
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());

        if (Boolean.FALSE.equals(isMember)) {
            // 2.1 如果未点赞，可以点赞
            // 3. 数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                // 3.2 保存用户到redis的set结合
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        } else {
            // 4. 如果已点赞，取消点赞
            // 4.1 数据库点赞数 - 1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                // 4.2 把用户从redis的set集合移除
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }

        return Result.ok();
    }
}
