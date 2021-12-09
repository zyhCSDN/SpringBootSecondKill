package com.debug.kill.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
