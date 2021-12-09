package com.debug.kill.server.dto;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * @author: Zhaoyongheng
 * @date: 2020/9/9
 */

@Data
public class UserEntity implements Serializable {
    private Long id;
    private String guid;
    private String name;
    private String age;
    private Date createTime;


}
