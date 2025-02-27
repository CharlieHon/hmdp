package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {


    /**
     * 首页博客查询功能
     * @param current 页数
     * @return
     */
    Result queryHotBlog(Integer current);


    /**
     * 笔记查询功能
     * @param id
     * @return
     */
    Result queryBlogById(Long id);

    /**
     * 笔记点赞功能
     * @param id 笔记id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 根据id查询笔记点赞列表
     * @param id 笔记id
     * @return 点赞列表
     */
    Result queryBlogLikes(Long id);

    /**
     * 发布笔记，并将笔记推送到粉丝的收件箱中
     * @param blog 笔记
     * @return
     */
    Result saveBlog(Blog blog);
}
