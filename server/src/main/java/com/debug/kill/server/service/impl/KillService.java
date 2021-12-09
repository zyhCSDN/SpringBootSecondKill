package com.debug.kill.server.service.impl;/**
 * Created by Administrator on 2019/6/17.
 */

import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.entity.ItemKill;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillMapper;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.enums.SysConstant;
import com.debug.kill.server.service.CheckService;
import com.debug.kill.server.service.IKillService;
import com.debug.kill.server.service.RabbitSenderService;
import com.debug.kill.server.utils.RandomUtil;
import com.debug.kill.server.utils.SnowFlake;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Author:zyh
 * @Date: 2019/6/17 22:21
 **/
@Service
public class KillService implements IKillService {

    private static final Logger log= LoggerFactory.getLogger(KillService.class);

    private SnowFlake snowFlake=new SnowFlake(2,3);

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private ItemKillMapper itemKillMapper;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    @Autowired
    private Environment env;


    @Autowired
    private CheckService checkService;

    /**
     * lock尝试获取锁的次数
     */
    private int retryCount = 3;

    /**
     * 每次尝试获取锁的重试间隔毫秒数
     */
    private int waitIntervalInMS = 100;

    /**
     * 商品秒杀核心业务逻辑的处理
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItem(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        //TODO:判断当前用户是否已经抢购了当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:判断当前代抢购的商品库存是否充足、以及是否出在可抢的时间段内 - canKill
            ItemKill itemKill=itemKillMapper.selectById(killId);
            if (itemKill!=null && 1==itemKill.getCanKill()){

                //TODO:扣减库存-减1
                int res=itemKillMapper.updateKillItem(killId);
                if (res>0){
                    //TODO:判断是否扣减成功了?是-生成秒杀成功的订单、同时通知用户秒杀已经成功（在一个通用的方法里面实现）
                    this.commonRecordKillSuccessInfo(itemKill,userId);

                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了！");
        }

        return result;
    }






    /**
     * 通用的方法-记录用户秒杀成功后生成的订单-并进行异步邮件消息的通知
     * @param kill
     * @param userId
     * @throws Exception
     */
    private void commonRecordKillSuccessInfo(ItemKill kill, Integer userId) throws Exception{
        //TODO:记录抢购成功后生成的秒杀订单记录
        ItemKillSuccess entity=new ItemKillSuccess();
        String orderNo=String.valueOf(snowFlake.nextId());
//        entity.setCode(RandomUtil.generateOrderCode());   //传统时间戳+N位随机数
        entity.setCode(orderNo); //雪花算法
        entity.setItemId(kill.getItemId());
        entity.setKillId(kill.getId());
        entity.setUserId(userId.toString());
        //TODO 此处订单付款状态，可调用随机方法，返回随机是否付款，便于测试
//        entity.setStatus(checkPay());
        entity.setStatus(SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue());
        entity.setCreateTime(DateTime.now().toDate());
        //TODO:学以致用，举一反三 -> 仿照单例模式的双重检验锁写法
        if (itemKillSuccessMapper.countByKillUserId(kill.getId(),userId) <= 0){
            int res=itemKillSuccessMapper.insertSelective(entity);
            if (res>0){
                //TODO:进行异步邮件消息的通知=rabbitmq+mail
                //没有付款的发送邮件提醒付款
                if(entity.getStatus()==SysConstant.OrderStatus.SuccessNotPayed.getCode().byteValue()){
                    rabbitSenderService.sendKillSuccessEmailMsg(orderNo);
                    //TODO:入死信队列，用于 “失效” 超过指定的TTL时间时仍然未支付的订单
                    rabbitSenderService.sendKillSuccessOrderExpireMsg(orderNo);
                }
            }
        }
    }



    /**
     * 商品秒杀核心业务逻辑的处理-mysql的优化
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV2(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        //TODO:判断当前用户是否已经抢购过当前商品
        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
            //TODO:A.查询待秒杀商品详情
            ItemKill itemKill=itemKillMapper.selectByIdV2(killId);

            //TODO:判断是否可以被秒杀canKill=1?
            if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                //TODO:B.扣减库存-减一
                int res=itemKillMapper.updateKillItemV2(killId);

                //TODO:扣减是否成功?是-生成秒杀成功的订单，同时通知用户秒杀成功的消息
                if (res>0){
                    commonRecordKillSuccessInfo(itemKill,userId);

                    result=true;
                }
            }
        }else{
            throw new Exception("您已经抢购过该商品了!");
        }
        return result;
    }



    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 商品秒杀核心业务逻辑的处理-redis的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV3(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){

            //TODO:借助Redis的原子操作实现分布式锁-对共享操作-资源进行控制
            ValueOperations valueOperations=stringRedisTemplate.opsForValue();
            final String key=new StringBuffer().append(killId).append(userId).append("-RedisLock").toString();
            final String value=RandomUtil.generateOrderCode();
            Boolean cacheRes=valueOperations.setIfAbsent(key,value); //lua脚本提供“分布式锁服务”，就可以写在一起
            //TOOD:redis部署节点宕机了
            if (cacheRes){
                stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);

                try {
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);

                            result=true;
                        }
                    }
                }catch (Exception e){
                    throw new Exception("还没到抢购日期、已过了抢购时间或已被抢购完毕！");
                }finally {
                    if (value.equals(valueOperations.get(key).toString())){
                        stringRedisTemplate.delete(key);
                    }
                }
            }
        }else{
            throw new Exception("Redis-您已经抢购过该商品了!");
        }
        return result;
    }




    @Autowired
    private RedissonClient redissonClient;

    /**
     *
     * Redisson 的 WatchDog 看门狗自动延期机制
     * 商品秒杀核心业务逻辑的处理-redisson的分布式锁
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean killItemV4(Integer killId, Integer userId) throws Exception {
        Boolean result=false;

        final String lockKey=new StringBuffer().append(killId).append(userId).append("-RedisSonLock").toString();
       //给lockKey字段加锁
        RLock lock=redissonClient.getLock(lockKey);

        // https://blog.csdn.net/u012693016/article/details/108244005 分布式锁案例可以看
        // https://www.cnblogs.com/nov5026/p/10764068.html 分布式锁实现原理可以看
        // https://www.cnblogs.com/yjmyzz/p/distribution-lock-using-redis.html 分布式锁实现原理可以看
        // 锁30秒后自动释放，防止死锁 这行代码的作用是，如果加锁的业务代码运行时间超过了30秒，便会自动释放锁，这是为防止业务代码运行时间过长或者出错导致死锁
//           lock.lock(30, TimeUnit.SECONDS);
        try {
            /**
             * tryLock(long waitTime, long leaseTime, TimeUnit unit) 设置传入的锁过期时间，并立即尝试获取锁。
             * 如果返回true则表明加锁成功，否则为加锁失败。
             * 注意：加锁的时间要大于业务执行时间，这个时间需要通过测试算出最合适的值，
             * 否则会造成加锁失败或者业务执行效率过慢等问题。
             */

//            并发请求，不论哪一条都必须要处理的场景(即：不允许丢数据)
//
//            比如：一个订单，客户正在前台修改地址，管理员在后台同时修改备注。地址和备注字段的修改，都必须正确更新，这二个请求同时到达的话，如果不借助db的事务，很容易造成行锁竞争，但用事务的话，db的性能显然比不上redis轻量。
//
//            解决思路：A,B二个请求，谁先抢到分布式锁（假设A先抢到锁），谁先处理，抢不到的那个（即：B），在一旁不停等待重试，重试期间一旦发现获取锁成功，即表示A已经处理完，把锁释放了。这时B就可以继续处理了。
//
//            但有二点要注意：
//
//            a、需要设置等待重试的最长时间，否则如果A处理过程中有bug，一直卡死，或者未能正确释放锁，B就一直会等待重试，但是又永远拿不到锁。
//
//            b、等待最长时间，必须大于锁的过期时间。否则，假设锁2秒过期自动释放，但是a还没处理完（即：a的处理时间大于2秒），这时锁会因为redis key过期“提前”误释放，B重试时拿到锁，造成A,B同时处理。（注：可能有同学会说，不设置锁的过期时间，不就完了么？理论上讲，确实可以这么做，但是如果业务代码有bug，导致处理完后没有unlock，或者根本忘记了unlock，分布式锁就会一直无法释放。所以综合考虑，给分布式锁加一个“保底”的过期时间，让其始终有机会自动释放，更为靠谱）


            //TODO:第一个参数30s=表示尝试获取分布式锁，并且最大的等待获取锁的时间为30s
            //TODO:第二个参数10s=表示上锁之后，10s内操作完毕将自动释放锁 防止死锁
            Boolean cacheRes=lock.tryLock(50,10,TimeUnit.SECONDS);
            if (cacheRes){
                log.info("线程" + Thread.currentThread().getName() + "加锁" + lock.getName() + "成功");
                log.info("获取RedissonLock分布式锁[成功],lockName={}", lock.getName());
                //TODO:核心业务逻辑的处理
                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
                        int res=itemKillMapper.updateKillItemV2(killId);
                        if (res>0){
                            commonRecordKillSuccessInfo(itemKill,userId);
                            result=true;
                        }
                    }
                }else{
                    log.error("redisson-您已经抢购过该商品了!");
                }
            }else {
                log.info("获取RedisSonLock分布式锁[失败],lockName={}", lock.getName());
            }
        }finally {
            //TODO:释放锁
            lock.unlock();
            //lock.forceUnlock();
            log.info("执行完毕，释放锁！");
        }

        return result;
    }


//
//    @Autowired
//    private CuratorFramework curatorFramework;

    private static final String pathPrefix="/kill/zkLock/";

//    /**
//     * 商品秒杀核心业务逻辑的处理-基于ZooKeeper的分布式锁
//     * @param killId
//     * @param userId
//     * @return
//     * @throws Exception
//     */
//    @Override
//    public Boolean killItemV5(Integer killId, Integer userId) throws Exception {
//        Boolean result=false;
//
//        InterProcessMutex mutex=new InterProcessMutex(curatorFramework,pathPrefix+killId+userId+"-lock");
//        try {
//            if (mutex.acquire(10L,TimeUnit.SECONDS)){
//
//                //TODO:核心业务逻辑
//                if (itemKillSuccessMapper.countByKillUserId(killId,userId) <= 0){
//                    ItemKill itemKill=itemKillMapper.selectByIdV2(killId);
//                    if (itemKill!=null && 1==itemKill.getCanKill() && itemKill.getTotal()>0){
//                        int res=itemKillMapper.updateKillItemV2(killId);
//                        if (res>0){
//                            commonRecordKillSuccessInfo(itemKill,userId);
//                            result=true;
//                        }
//                    }
//                }else{
//                    throw new Exception("zookeeper-您已经抢购过该商品了!");
//                }
//            }
//        }catch (Exception e){
//            throw new Exception("还没到抢购日期、已过了抢购时间或已被抢购完毕！");
//        }finally {
//            if (mutex!=null){
//                mutex.release();
//            }
//        }
//        return result;
//    }




    /**
     * 模拟判断订单支付成功或未付款，成功失败随机
     * @param
     * @return
     */
    public static byte checkPay(){
        Random random = new Random();
//        该方法的作用是生成一个随机的int值，该值介于[0,n)的区间，也就是0到n之间的随机int值，包含0而不包含n。
        byte res = (byte)random.nextInt(2);
        log.info("res======================"+  res  );
      return res;
    }

//    public static void main(String[] args) {
//        checkPay();
//    }



    /**
     * 检查用户的秒杀结果
     * @param killId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public Map<String,Object> checkUserKillResult(Integer killId, Integer userId) throws Exception {
        Map<String,Object> dataMap= Maps.newHashMap();
        List<KillSuccessUserInfo> infoList=itemKillSuccessMapper.selectByKillIdUserId(killId,userId);
        if (!infoList.isEmpty() && infoList.size()>0){
            for (KillSuccessUserInfo info : infoList) {
                dataMap.put("executeResult",String.format(env.getProperty("notice.kill.item.success.content"),info.getItemName()));
                dataMap.put("info",info);
            }
        }else{
            throw new Exception(env.getProperty("notice.kill.item.fail.content"));
        }
        return dataMap;
    }
}








































