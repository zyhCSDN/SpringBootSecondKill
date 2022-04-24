package com.debug.kill.server.controller;

import com.debug.kill.server.dto.UserEntity;
import com.debug.kill.server.utils.RedisUtil;
import com.debug.kill.server.utils.SnowFlake;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;


/**
 * @author: Zhaoyongheng
 * @date: 2020/9/9
 */

@Component
@RequestMapping("/redis")
@RestController
@Api(tags = "redis测试")
public class RedisController {

   private SnowFlake snowFlake = new SnowFlake(2, 3);

    private static Logger log = LoggerFactory.getLogger(RedisController.class);

    private static int ExpireTime = 60;   // redis中存储的过期时间60s

    @Resource
    private RedisUtil redisUtil;

        @RequestMapping("set")
//    @Scheduled(cron = "0/5 * * * * ? ") // 间隔5秒执行
    public boolean redisset() {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        log.info("------------------------------");
        Map< String, Object > hashMap = new HashMap<>();
        long l = snowFlake.nextId();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(Long.valueOf(l));
        userEntity.setGuid(valueOf(1));
        userEntity.setName("zhangsan");
        userEntity.setAge(valueOf(20));
        userEntity.setCreateTime(new Date());

        hashMap.put("id", valueOf(l));
        hashMap.put("name", "zhangsan");
        hashMap.put("age", "12");
        hashMap.put("date", simpleDateFormat.format(new Date()));
        return redisUtil.hmset(valueOf(l) + ":" + "userEntity", hashMap);
    }


    @RequestMapping("get")
    public Object redisget(String key) {
        return redisUtil.hmget(key);
    }

    @RequestMapping("expire")
    public boolean expire(String key) {
        return redisUtil.expire(key, ExpireTime);
    }

    @RequestMapping("getExpire")
    public Long getExpire(String key) {
        return redisUtil.getExpire(key);
    }

}
