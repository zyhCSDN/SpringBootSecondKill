package com.debug.kill.server.service;

import com.debug.kill.server.dto.KillDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


/**
 * 校验类service
 */
@Service("checkService")
public class CheckService {

    @Autowired
    private Environment env;

//   注入这个RedisTemplate存储key乱码
//    @Autowired
//    private RedisTemplate redisTemplate;

    @Autowired
    private StringRedisTemplate StringRedisTemplate;


    /**
     * 校验同一用户对同一商品是否重复下单并保持到redis ture 未下单，正常，false,已下单，异常
     * @param order 下单参数封装
     * @return true-通过校验  false-未通过
     */
    public Boolean checkSeckillUser(KillDto order, Integer i) {

        String key = env.getProperty("seckill.redis.key.prefix") + order.getUserId() + order.getKillId() + i;
        /**
         * 1.返回ture,表明该用户与该商品第一次匹配进redis,即该用户首次秒杀该商品
         * 2.利用redis的setnx原理，避免多服务、高并发带来的线程安全问题
         * 3.一行代码搞定
         * 4.没有太具体的业务场景，不设置过期时间(可以设置过期时间，60s秒，60秒之后，redis里的信息自动删除)
         */
        String property = env.getProperty( "spring.redis.timeout" );
//        Long aLong = Long.valueOf(property)/1000;
//        return StringRedisTemplate.opsForValue().setIfAbsent(key,Integer.toString( i ), aLong, TimeUnit.SECONDS);
        return StringRedisTemplate.opsForValue().setIfAbsent(key,Integer.toString( i ));


        /* 老代码，代码冗余，未加锁，线程不安全
            String key = env.getProperty("seckill.redis.key.prefix")+userId;
            //用户秒杀过的商品id集合
            Set<String> goodsIdSet = redisTemplate.opsForSet().members(key);
            if(goodsIdSet != null && goodsIdSet.contains(goodsId)){
                //不是第一次秒杀，返回false
                return false;
            }else{
                //是第一次秒杀，将该id添加到该用户的集合下面
                redisTemplate.opsForSet().add(key,goodsId);
                return true;
            }
        */
    }

}
