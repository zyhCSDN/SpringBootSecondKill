package com.debug.kill.server.service;/**
 * Created by Administrator on 2019/6/21.
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.entity.ItemKillSuccess;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.dto.MailDto;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ通用的消息接收服务
 * @Author:zyh
 * @Date: 2019/6/21 21:47
 **/
@Service
public class RabbitReceiverService {

    public static final Logger log= LoggerFactory.getLogger(RabbitReceiverService.class);

    @Autowired
    private MailService mailService;

    @Autowired
    private Environment env;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;

    @Autowired
    private IKillService killService;



    /**
     * 秒杀异步邮件通知-接收消息
     * 单一消费者
     */
    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"},containerFactory = "singleListenerContainer")
//    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"},containerFactory = "multiListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info){
        try {
            log.info("秒杀异步邮件通知-接收消息:{}",info);

            //TODO:真正的发送邮件....
            //简单文本
            //MailDto dto=new MailDto(env.getProperty("mail.kill.item.success.subject"),"这是测试内容",new String[]{info.getEmail()});
            //mailService.sendSimpleEmail(dto);
            //花哨文本
            final String content=String.format(env.getProperty("mail.kill.item.success.content"),info.getItemName(),info.getCode());
            MailDto dto=new MailDto(env.getProperty("mail.kill.item.success.subject"),content,new String[]{info.getEmail()});
            mailService.sendHTMLMail(dto);

        }catch (Exception e){
            log.error("秒杀异步邮件通知-接收消息-发生异常：",e.fillInStackTrace());
        }
    }

    /**
     * 用户秒杀成功后超时未支付-监听者
     * 通道，死信队列确认消费用
     *
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "multiListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info){
        try {
            log.info("用户秒杀成功后超时未支付-监听者-接收消息:{}",info);
            if (info!=null){
                //获取交付tag
//                long tag = message.getMessageProperties().getDeliveryTag();
//                String str = new String(message.getBody(),"utf-8");
//                KillSuccessUserInfo info = JSONUtil.toBean(str, KillSuccessUserInfo.class);
                ItemKillSuccess entity=itemKillSuccessMapper.selectByPrimaryKey(info.getCode());
                if (entity!=null && entity.getStatus().intValue()==0){
                    itemKillSuccessMapper.expireOrder(info.getCode());
                }
                //TODO 手动确认消息消费(此操作会把消息队列的数据条数清空)
//                channel.basicAck(tag,true);
//                log.info("手动确认订单超时未支付消费：{}",tag);
            }

        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }



    /**
     *
     * 以前的参数是 KillDto
     * 秒杀时异步接收Mq消息-监听者
     * @param message
     * @param channel 通道，确认消费用
     */
    @RabbitListener(queues = {"${mq.kill.item.execute.limit.queue.name}"},containerFactory = "multiListenerContainer")
    public void consumeKillExecuteMqMsg(Message message, Channel channel){
        try {
            if (message!=null){
                log.info("大批量请求秒杀时异步发送Mq消息接收成功：==============：{}",message.getBody());
//                todo https://www.cnblogs.com/blacksmith4/p/13407456.html
//              此步骤为上面url  channel.basicQos(1);//保证一次只消费一个消息
                //获取交付tag
                long tag = message.getMessageProperties().getDeliveryTag();
                //采用任何一种加分布锁的处理方法都是可行的
                //TODO 此步骤非常重要,Jmeter压测,发送抢单的大批量请求
                String str = new String(message.getBody(),"utf-8");
                KillDto dto = JSONObject.parseObject(str, KillDto.class);
//               todo 避免消息重复消费 方法一： 在消息消费时，要求消息体中必须要有一个唯一Id（对于同一业务全局唯一，如支付ID、订单ID、帖子ID等）
//                作为去重的依据，避免同一条消息被重复消费
//                 方法二：消费消息接口做幂等处理
                killService.killItemV4(dto.getKillId(),dto.getUserId());
                //TODO 手动确认消息消费(此操作会把消息队列的数据条数清空)
                channel.basicAck(tag,true);
                log.info("手动确认下单消息消费：{}",tag);
            }
        }catch (Exception e){
            log.error("用户秒杀成功后超时未支付-监听者-发生异常：",e.fillInStackTrace());
        }
    }
}












