package com.debug.kill.server.thread;/**
 * Created by Administrator on 2019/7/11.
 */

import com.debug.kill.model.entity.RandomCode;
import com.debug.kill.model.mapper.RandomCodeMapper;
import com.debug.kill.server.utils.RandomUtil;
import com.debug.kill.server.utils.SnowFlake;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author:debug (SteadyJack)
 * @Date: 2019/7/11 10:30
 **/
@Slf4j
public class CodeGenerateSnowThread implements Runnable {

    private static final SnowFlake SNOW_FLAKE = new SnowFlake(2, 3);

    private RandomCodeMapper randomCodeMapper;

    public CodeGenerateSnowThread(RandomCodeMapper randomCodeMapper) {
        this.randomCodeMapper = randomCodeMapper;
    }

    @Override
    public void run() {
        long l = SNOW_FLAKE.nextId();
        log.info("执行的线程名称name2:{},参数:{}",Thread.currentThread().getName(),l);
        RandomCode entity = new RandomCode();
        entity.setCode(String.valueOf(l));
        randomCodeMapper.insertSelective(entity);
    }
}