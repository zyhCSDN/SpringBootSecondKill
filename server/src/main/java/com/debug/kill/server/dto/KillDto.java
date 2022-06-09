package com.debug.kill.server.dto;/**
 * Created by Administrator on 2019/6/17.
 */

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Author:debug (SteadyJack)
 * @Date: 2019/6/17 22:18
 **/
@Data
@ToString
public class KillDto implements Serializable {

    //全局唯一id,避免消息被重复消费
    private Long globallyNniQueId;

    @NotNull
    private Integer killId;

    private Integer userId;


}