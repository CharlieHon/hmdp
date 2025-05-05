package com.hmdp.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
    // 针对于商家，其对应店铺的id
    private Long shopId;
}
