package com.debug.kill.server.thread;/**
 * Created by Administrator on 2019/7/11.
 */

import com.debug.kill.model.entity.RandomCode;
import com.debug.kill.model.mapper.RandomCodeMapper;
import com.debug.kill.server.enums.Constant;
import com.debug.kill.server.utils.RandomUtil;
import com.debug.kill.server.utils.RedisUtil;
import com.debug.kill.server.utils.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * @Author:debug (SteadyJack)
 * @Date: 2019/7/11 10:30
 **/
@Slf4j
public class CodeGenerateSnowThread implements Runnable {


    private RedisTemplate redisTemplate;


    private static final SnowFlake SNOW_FLAKE = new SnowFlake(2, 3);

    private RandomCodeMapper randomCodeMapper;

    public CodeGenerateSnowThread(RandomCodeMapper randomCodeMapper, RedisTemplate redisTemplate) {
        this.randomCodeMapper = randomCodeMapper;
        this.redisTemplate = redisTemplate;
    }

    ListOperations<String, Long> listOperations = redisTemplate.opsForList();

    @Override
    public void run() {
        long l = SNOW_FLAKE.nextId();
        log.info("执行的线程名称name2:{},参数:{}", Thread.currentThread().getName(), l);
        RandomCode entity = new RandomCode();
        entity.setCode(String.valueOf(l));
//        ValueOperations valueOperations = redisTemplate.opsForValue();
//        valueOperations.set(String.valueOf(l),String.valueOf(l));
        listOperations.leftPush(Constant.RedisListPrefix + l, l);
//        randomCodeMapper.insertSelective(entity);
    }
}