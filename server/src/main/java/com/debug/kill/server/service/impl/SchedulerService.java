package com.debug.kill.server.service.impl;

import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author ZhaoYongHeng
 * @date 2020/12/14
 */

@Service
@Slf4j
public class SchedulerService {

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private Environment env;

    //定时获取status=0的订单并判断是否超过TTL，然后进行失效
    //每30秒执行一次
    @Scheduled(cron = "0/30 * * * * ?")
    public void schedulerExpireOrders() {
        try {
            List<ItemKillSuccess> list = itemKillSuccessMapper.selectExpireOrders();
            if (list != null && !list.isEmpty()) {
                //java8的写法
                list.stream().forEach(i -> {
                    if (i != null && i.getDiffTime() > env.getProperty("scheduler.expire.orders.time", Integer.class)) {
                        itemKillSuccessMapper.expireOrder(i.getCode());
                        log.info("定时获取status=0的订单并判断是否超过TTL(此步骤为用户下单成功但是超时30分钟后未付款设置状态为无效)");
                    }
                });
            } else {
                log.info("查询到列表list为空");
            }
        } catch (Exception e) {
            log.error("定时获取status=0的订单并判断是否超过TTL，然后进行失效-发生异常：", e.fillInStackTrace());
        }
    }
}