package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

public class UserHolder {

    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    public static void saveUser(UserDTO user){
        // 测试：每个请求都对应这一个新的线程，而tl因为是 static final 类型，是共享变量
        // Thread[http-nio-8081-exec-7]-[java.lang.ThreadLocal@33bd31ab]
        // Thread[http-nio-8081-exec-9]-[java.lang.ThreadLocal@33bd31ab]
        // System.out.println("Thread[" + Thread.currentThread().getName() + "]-[" + tl + "]");
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
