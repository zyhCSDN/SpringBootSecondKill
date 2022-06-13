package com.debug.kill.server.config;

import com.debug.kill.server.service.RabbitSenderService;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * 通用化 Rabbitmq 配置
 */
@Configuration
//@Slf4j
public class RabbitmqConfig {

    private final static Logger log = LoggerFactory.getLogger(RabbitmqConfig.class);

    @Autowired
    private Environment env;

    @Autowired
    private CachingConnectionFactory connectionFactory;

    @Autowired
    private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;

    @Autowired
    private RabbitSenderService rabbitSenderService;

    /**
     * ******************************消息发送规则****************************
     *
     * 生产者把消息发布到交换器上，消息从交换器到达特定的队列需要进行绑定，消息最终到达队列并被消费者接收
     * 队列是AMQP消息通信的基础模块，它为消息提供了住所，消息在此等待消费。然而，消息是如何到达队列的呢？
     *
     * bind(executeLimitQueue()).to(executeLimitExchange()).with(env.getProperty("mq.kill.item.execute.limit.queue.routing.key"));
     * 消息发送给交换器后，根据确定的规则（路由键），RabbitMQ将会决定消息该投递到哪个队列；队列通过路由键绑定到交换器，
     * 当消息发送到RabbitMQ时，消息将拥有一个路由键，RabbitMQ会将其和绑定使用的路由键进行匹配，如果相匹配的话，那么消息将会投递到该队列；
     * 如果路由的消息不匹配任何绑定模式的话，消息将进入“黑洞”。
     *
     * 如上所述，RabbitMQ会根据路由键将消息从交换器路由到队列，但它如何处理投递到多个队列的情况呢？此时，
     * 不同类型的交换器就发挥作用啦。RabbitMQ的交换器类型一共有四种（direct、fanout、topic以及headers），
     * 每一种类型实现了不同的路由算法，其中direct类型交换器非常简单，当声明一个队列时，
     * 它会自动绑定到direct类型交换器（默认条件下，是一个空白字符串名称的交换器），
     * 并以队列名称作为路由键；当消息发送到RabbitMQ后所拥有的路由键与绑定使用的路由键匹配，消息就被投递到对应的队列。
     * headers交换器和direct交换器完全一致，但性能会差很多，headers交换器允许匹配AMQP消息的是header而非路由键，因此它并不实用。
     * fanout交换器可以将收到的消息投递给所有附件在此交换器上的队列。
     * topic交换器可以使得来自不同源头的消息能够到达同一个队列。
     *
     */


    /**
     * 单一消费者
     *
     * @return
     */
    @Bean(name = "singleListenerContainer")
    public SimpleRabbitListenerContainerFactory listenerContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(1);
        factory.setPrefetchCount(1);
        factory.setTxSize(1);
        log.info("调用单个消费者开始(此为邮件接收者RabbitReceiverService单一消费者配置)：{}", factory);
        return factory;
    }

    /**
     * 多个消费者
     *
     * @return
     */
    @Bean(name = "multiListenerContainer")
    public SimpleRabbitListenerContainerFactory multiListenerContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        //确认消费模式-NONE(自动确认)-MANUAL(手动确认)
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.concurrency", int.class));
        factory.setMaxConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.max-concurrency", int.class));
        factory.setPrefetchCount(env.getProperty("spring.rabbitmq.listener.simple.prefetch", int.class));
        log.info("调用多个消费者开始(此为异步秒杀MQ请求与用户秒杀成功后超时未支付-监听者 多个消费者配置)：{}", factory);
        return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        // https://juejin.cn/post/6844903924642611207
        //TODO 配置文件可以不用配置  手动配置确保消息正确地发送至RabbitMQ 将 channel 设置成 confirm 模式（发送方确认模式，消息确认发送）
//        //消息发送成功确认，对应application.properties中的spring.rabbitmq.publisher-confirms=true
//        connectionFactory.setPublisherConfirms(true);
//        //消息发送失败确认，对应application.properties中的spring.rabbitmq.publisher-returns=true
//        connectionFactory.setPublisherReturns(true);
        connectionFactory.setPublisherConfirms(true);
        connectionFactory.setPublisherReturns(true);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMandatory(true);
        //消息发送到exchange回调 需设置：spring.rabbitmq.publisher-confirms=true
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                log.info("消息发送成功:correlationData({}),ack({}),cause({})", correlationData, ack, cause);
            }
        });
        //消息从exchange发送到queue失败回调  需设置：spring.rabbitmq.publisher-returns=true
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            //            @SneakyThrows
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                log.warn("消息丢失:exchange({}),route({}),replyCode({}),replyText({}),message:{}", exchange, routingKey, replyCode, replyText, message);
                //todo 消息丢失，重新发送
//                String str = new String(message.getBody(),"utf-8");
//                KillDto dto = JSONUtil.toBean(str, KillDto.class);
//                rabbitSenderService.sendKillExecuteMqMsg(dto);
            }
        });
        return rabbitTemplate;
    }


    //构建异步发送邮箱通知的消息模型
    @Bean
    public Queue successEmailQueue() {
        return new Queue(env.getProperty("mq.kill.item.success.email.queue"), true);
    }

    @Bean
    public TopicExchange successEmailExchange() {
        return new TopicExchange(env.getProperty("mq.kill.item.success.email.exchange"), true, false);
    }

    @Bean
    public Binding successEmailBinding() {
        return BindingBuilder.bind(successEmailQueue()).to(successEmailExchange()).with(env.getProperty("mq.kill.item.success.email.routing.key"));
    }



    //构建秒杀成功之后-订单超时未支付的死信队列消息模型
    @Bean
    public Queue successKillDeadQueue() {
        Map<String, Object> argsMap = Maps.newHashMap();
        argsMap.put("x-dead-letter-exchange", env.getProperty("mq.kill.item.success.kill.dead.exchange"));
        argsMap.put("x-dead-letter-routing-key", env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.queue"), true, false, false, argsMap);
    }

    //基本交换机
    @Bean
    public TopicExchange successKillDeadProdExchange() {
        return new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"), true, false);
    }

    //创建基本交换机+基本路由 -> 死信队列 的绑定
    @Bean
    public Binding successKillDeadProdBinding() {
        return BindingBuilder.bind(successKillDeadQueue()).to(successKillDeadProdExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
    }

    //真正的队列
    @Bean
    public Queue successKillRealQueue() {
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.real.queue"), true);
    }

    //死信交换机
    @Bean
    public TopicExchange successKillDeadExchange() {
        return new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.exchange"), true, false);
    }

    //死信交换机+死信路由->真正队列 的绑定
    @Bean
    public Binding successKillDeadBinding() {
        return BindingBuilder.bind(successKillRealQueue()).to(successKillDeadExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
    }

    /**
     * (构造一个新队列，给定名称、持久性标志、自动删除标志和参数。)
     * name 队列的名称-不能为null；设置为“”以让代理生成该名称。
     * durable 如果声明持久队列，则为true（该队列将在服务器重新启动后继续有效）
     * exclusive 如果声明独占队列，则为true（该队列将仅由声明者的连接使用）
     * autoDelete 服务器应在队列不再使用时将其删除，则为true
     * arguments 用于声明队列的参数
     */
    //TODO:RabbitMQ限流专用
    @Bean
    public Queue executeLimitQueue() {
        Map<String, Object> argsMap = Maps.newHashMap();
        //限制channel中队列同一时刻通过的消息数量
        argsMap.put("x-max-length", env.getProperty("spring.rabbitmq.listener.simple.prefetch", Integer.class));
        return new Queue(env.getProperty("mq.kill.item.execute.limit.queue.name"), true, false, false, argsMap);
    }

    @Bean
    public TopicExchange executeLimitExchange() {
        return new TopicExchange(env.getProperty("mq.kill.item.execute.limit.queue.exchange"), true, false);
    }

    @Bean
    public Binding executeLimitBinding() {
        return BindingBuilder.bind(executeLimitQueue()).to(executeLimitExchange()).with(env.getProperty("mq.kill.item.execute.limit.queue.routing.key"));
    }
}






























































































