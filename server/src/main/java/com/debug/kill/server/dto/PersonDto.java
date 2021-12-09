package com.debug.kill.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author: Zhaoyongheng
 * @date: 2019/9/26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class PersonDto implements Serializable {
    private Integer id;

    private Integer age;

    private String name;

}
