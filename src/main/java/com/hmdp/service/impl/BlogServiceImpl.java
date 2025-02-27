package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static com.hmdp.utils.RedisConstants.FEED_KEY;

/**
 * <p>
 * 服务实现类
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

    private final IFollowService followService;

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
     *
     * @param blog 笔记
     */
    private void isBlogLiked(Blog blog) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录（如访问首页面时），直接返回
            return;
        }
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        // Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        // blog.setIsLike(Boolean.TRUE.equals(isMember));
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }

    /**
     * 填充笔记用户信息
     *
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
        String key = BLOG_LIKED_KEY + id;
        // Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        // sorted_set中通过查询用户的分树(score)来判断元素是否存在，如果score为null，则代表不存在，反之存在
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            // 2.1 如果未点赞，可以点赞
            // 3. 数据库点赞数 + 1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            if (isSuccess) {
                // // 3.2 保存用户到redis的set结合
                // stringRedisTemplate.opsForSet().add(key, userId.toString());
                // 3.3 为实现点赞顺序排序，改用sorted_set存储，score为时间戳
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4. 如果已点赞，取消点赞
            // 4.1 数据库点赞数 - 1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            if (isSuccess) {
                // 4.2 把用户从redis的set集合移除
                // stringRedisTemplate.opsForSet().remove(key, userId.toString());
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            // 没有人点赞，返回空集合
            return Result.ok(Collections.emptyList());
        }
        // 2. 解析其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        // 3. 根据用户id查询用户 listByIds(ids) --> select * from tb_user where id in (1010, 5) 结果不会按照 () 中id顺序给出
        // 🔺解决方案： select * from tb_user where id in (1010, 5) order by field(id, 1010, 5)
        String idStr = StrUtil.join(",", ids);  // 1010,5
        List<UserDTO> userDTOS = userService
                // .listByIds(ids)
                .query().in("id", ids).last("ORDER BY FIELD (id," + idStr + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        Long userId = UserHolder.getUser().getId();
        blog.setUserId(userId);
        // 保存探店博文
        boolean isSuccess = save(blog);
        // 把笔记推送给粉丝
        if (!isSuccess) {
            return Result.fail("发送笔记失败");
        }

        // select * from tb_follow where follow_user_id = userId
        List<Follow> follows = followService.query().eq("follow_user_id", userId).list();
        follows.stream()
                .map(Follow::getUserId) // 获取粉丝id
                .forEach(userId1 -> {   // 将笔记推送到 key 为 feed:粉丝id 的 sorted_set
                    String key = FEED_KEY + userId1;
                    stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
                });
        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Long offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();

        // 2. 查询收件箱 zrevrangebyscore key max min
        String key = FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);

        // 3. 非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }

        // 4. 解析数据：blogId, score(时间戳)，offset(偏移量，)
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;   // 最小时间戳
        int os = 1;         // 偏移量，即score等于最小时间戳的个数
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 4.1 获取id
            ids.add(Long.valueOf(tuple.getValue()));
            // 4.2 获取分数（时间戳），最后一个元素即为最小时间戳
            long time = tuple.getScore().longValue();
            if (time == minTime) {
                os++;
            } else {
                minTime = time;
                os = 1;
            }
        }

        String idsStr = StrUtil.join(",", ids);
        // 5.1 根据id查询blog
        List<Blog> blogs = query().in("id", ids).last("order by field(id, " + idsStr + " )").list();
        // 5.2 查询blog用户和是否被点赞
        blogs.forEach(
                blog -> {
                    queryBlogUser(blog);    // 填充笔记的用户信息
                    isBlogLiked(blog);      // 查询笔记是否被当前用户顶赞
                }
        );

        // 5. 封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setMinTime(minTime);
        r.setOffset(os);
        return Result.ok(r);
    }
}
