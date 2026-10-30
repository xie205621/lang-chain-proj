package com.xxx.proj.vo;

import lombok.Data;

@Data
public class ChatForm {
    private Long memoryId;//对话id
    private String message;//用户问题
    private String userId;//持久化用户标识（浏览器级别，跨对话不变）
}