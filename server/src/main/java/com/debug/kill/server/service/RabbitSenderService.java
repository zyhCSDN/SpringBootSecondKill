package com.debug.kill.server.service;/**
 * Created by Administrator on 2019/6/21.
 */

import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.utils.SnowFlake;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ通用的消息发送服务
 *
 * @Author:zyh
 * @Date: 2019/6/21 21:47
 **/
@Service
public class RabbitSenderService {


    private static final SnowFlake SNOW_FLAKE = new SnowFlake(2, 3);

    public static final Logger log = LoggerFactory.getLogger(RabbitSenderService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Environment env;

    @Autowired
    private ItemKillSuccessMapper itemKillSuccessMapper;


    /**
     * TODO 此步为限流
     * 秒杀时异步发送Mq消息
     *
     * @param killDto
     */
    public void sendKillExecuteMqMsg(final KillDto killDto) {
        log.info("大批量秒杀请求秒杀时异步发送Mq消息======{}", killDto);
        try {
            if (killDto != null) {
                killDto.setGloballyNniQueId(SNOW_FLAKE.nextId());
                rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                rabbitTemplate.setExchange(env.getProperty("mq.kill.item.execute.limit.queue.exchange"));
                rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.execute.limit.queue.routing.key"));
                rabbitTemplate.convertAndSend(killDto, new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        MessageProperties mp = message.getMessageProperties();
                        mp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        //               todo 避免消息重复消费 方法一： 在消息消费时，要求消息体中必须要有一个唯一Id
                        //                （对于同一业务全局唯一，如支付ID、订单ID、帖子ID等）作为去重的依据，避免同一条消息被重复消费
                        mp.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillDto.class);
                        log.info("下单时候的message========{}", message);
                        return message;
                    }
                });
            }
        } catch (Exception e) {
            log.error("秒杀时异步发送Mq消息-发生异常，消息为：{}", killDto, e.fillInStackTrace());
        }
    }

    /**
     * 秒杀成功异步发送邮件通知消息
     */
    public void sendKillSuccessEmailMsg(String orderNo) {
        log.info("秒杀成功异步发送邮件通知消息-准备发送消息：{}", orderNo);
        try {
            if (StringUtils.isNotBlank(orderNo)) {
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderNo);
                if (info != null) {
                    //TODO:rabbitmq发送消息的逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.email.exchange"));
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.email.routing.key"));

                    //TODO：将info充当消息发送至队列
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            MessageProperties messageProperties = message.getMessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            log.info("邮件通知的message========{}", message);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("秒杀成功异步发送邮件通知消息-发生异常，消息为：{}", orderNo, e.fillInStackTrace());
        }
    }


    /**
     * 秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效的超时未支付的订单
     *
     * @param orderCode
     */
    public void sendKillSuccessOrderExpireMsg(final String orderCode) {
        try {
            log.info("用户秒杀成功后超时未支付-监听者-发送消息:{}", orderCode);
            if (StringUtils.isNotBlank(orderCode)) {
                KillSuccessUserInfo info = itemKillSuccessMapper.selectByCode(orderCode);
                if (info != null) {
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"));
                    rabbitTemplate.setRoutingKey(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) {
                            MessageProperties mp = message.getMessageProperties();
                            mp.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            mp.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            //TODO：动态设置TTL(为了测试方便，暂且设置60s) 60秒之后还没有支付，监听者设置订单失效 (时间可同redis或者schedulerExpireOrders定时任务一样)
                            mp.setExpiration(env.getProperty("mq.kill.item.success.kill.expire"));
                            log.info("死信队列的message========{}", message);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("秒杀成功后生成抢购订单-发送信息入死信队列，等待着一定时间失效超时未支付的订单-发生异常，消息为：{}", orderCode, e.fillInStackTrace());
        }
    }
}




























