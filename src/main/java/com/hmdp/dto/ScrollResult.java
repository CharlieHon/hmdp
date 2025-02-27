package com.hmdp.dto;

import lombok.Data;

import java.util.List;

/**
 * Feeds流滚动分页结果
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
