package com.debug.kill.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Zhaoyongheng
 * @version 1.0.0
 * @date 2021/1/21
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Student {
    private String name;
    private Integer age;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
