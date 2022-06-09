package com.debug.kill.server.controller;/**
 * Created by Administrator on 2019/6/17.
 */

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.model.dto.KillSuccessUserInfo;
import com.debug.kill.model.mapper.ItemKillSuccessMapper;
import com.debug.kill.server.dto.KillDto;
import com.debug.kill.server.service.CheckService;
import com.debug.kill.server.service.IKillService;
import com.debug.kill.server.service.RabbitSenderService;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Objects;

/**
 * 秒杀controller
 *
 * @Author:zyh
 * @Date: 2019/6/17 22:14
 **/
@Controller
@Api(tags = "秒杀列表")
public class KillController {

    private static final Logger log = LoggerFactory.getLogger(KillController.class);

    private static final String prefix = "kill";

    @Autowired
    private IKillService killService;

    @Autowired
    private ItemKillSuccessMapper ItemKillSuccessMapper;

    /***
     * 商品秒杀核心业务逻辑
     * @param dto
     * @param result
     * @return
     */
    @RequestMapping(value = prefix + "/execute", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse execute(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.INVALID_PARAMS);
        }
        Integer userId = dto.getUserId();

        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        try {
            Boolean res = killService.killItemV3(dto.getKillId(), userId);
            if (!res) {
                return new BaseResponse(StatusCode.FAIL.getCode(), "哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.FAIL.getCode(), e.getMessage());
        }
        return response;
    }


    /***
     * 商品秒杀核心业务逻辑-用于压力测试
     * @param dto
     * @param result
     * @return
     */
    @RequestMapping(value = prefix + "/execute/lock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeLock(@RequestBody @Validated KillDto dto, BindingResult result) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.INVALID_PARAMS);
        }
        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        try {
            //不加分布式锁的前提
            /*Boolean res=killService.killItemV2(dto.getKillId(),dto.getUserId());
            if (!res){
                return new BaseResponse(StatusCode.FAIL.getCode(),"不加分布式锁-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }*/

            //基于Redis的分布式锁进行控制
            /*Boolean res=killService.killItemV3(dto.getKillId(),dto.getUserId());
            if (!res){
                return new BaseResponse(StatusCode.FAIL.getCode(),"基于Redis的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }*/

            //基于Redisson的分布式锁进行控制
            Boolean res = killService.killItemV4(dto.getKillId(), dto.getUserId());
            if (!res) {
                return new BaseResponse(StatusCode.FAIL.getCode(), "基于Redisson的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
            }

            //基于ZooKeeper的分布式锁进行控制
//            Boolean res=killService.killItemV5(dto.getKillId(),dto.getUserId());
//            if (!res){
//                return new BaseResponse(StatusCode.FAIL.getCode(),"基于ZooKeeper的分布式锁进行控制-哈哈~商品已抢购完毕或者不在抢购时间段哦!");
//            }

        } catch (Exception e) {
            response = new BaseResponse(StatusCode.FAIL.getCode(), e.getMessage());
        }
        return response;
    }


    //http://localhost:8083/kill/kill/record/detail/343147116421722112

    /**
     * 查看订单详情
     *
     * @return
     */
    @RequestMapping(value = prefix + "/record/detail/{orderNo}", method = RequestMethod.GET)
    public String killRecordDetail(@PathVariable String orderNo, ModelMap modelMap) {
        if (StringUtils.isBlank(orderNo)) {
            return "error";
        }
        KillSuccessUserInfo info = ItemKillSuccessMapper.selectByCode(orderNo);
        if (info == null) {
            return "error";
        }
        modelMap.put("info", info);
        return "killRecord";
    }


    //抢购成功跳转页面
    @RequestMapping(value = prefix + "/execute/SUCCESS", method = RequestMethod.GET)
    public String executeSUCCESS() {
        return "executeSUCCESS";
    }

    //抢购失败跳转页面
    @RequestMapping(value = prefix + "/execute/FAIL", method = RequestMethod.GET)
    public String executeFAIL(ModelMap modelMap) {
        String msg = "您已经秒杀过该商品，不能再次秒杀";
        modelMap.put("msg", msg);
        return "executeFAIL";
    }


    @Autowired
    private RabbitSenderService rabbitSenderService;


    @Autowired
    private CheckService checkService;

    //商品秒杀核心业务逻辑-mq限流
    @RequestMapping(value = prefix + "/execute/mq", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeMq(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.INVALID_PARAMS);
        }
        Object uId = session.getAttribute("uid");
        if (uId == null) {
            return new BaseResponse(StatusCode.USER_NOT_LOGIN);
        }
        Integer userId = (Integer) uId;

        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        Map<String, Object> dataMap = Maps.newHashMap();
        try {
            dataMap.put("killId", dto.getKillId());
            dataMap.put("userId", userId);
            dto.setUserId(userId);
            response.setData(dataMap);
            //todo sendKillExecuteMqMsg 方法可有返回值，异步处理
            rabbitSenderService.sendKillExecuteMqMsg(dto);
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.FAIL.getCode(), e.getMessage());
        }
        return response;
    }

    //商品秒杀核心业务逻辑-mq限流-立马跳转至抢购结果页
    @RequestMapping(value = prefix + "/execute/mq/to/result", method = RequestMethod.GET)
    public String executeToResult(@RequestParam Integer killId, HttpSession session, ModelMap modelMap) {
        Object uId = session.getAttribute("uid");
        if (uId != null) {
            Integer userId = (Integer) uId;

            modelMap.put("killId", killId);
            modelMap.put("userId", userId);
        }
        return "executeMqResult";
    }

    //商品秒杀核心业务逻辑-mq限流-在抢购结果页中发起抢购结果的查询
    @RequestMapping(value = prefix + "/execute/mq/result", method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse executeResult(@RequestParam Integer killId, @RequestParam Integer userId) {
        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        try {
            Map<String, Object> resMap = killService.checkUserKillResult(killId, userId);
            response.setData(resMap);
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.FAIL.getCode(), e.getMessage());
        }
        return response;
    }


    // TODO 以上方法都可以不要,下面这个mq限流最重要
    //商品秒杀核心业务逻辑-mq限流-JMeter压测
    @RequestMapping(value = prefix + "/execute/mq/lock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public BaseResponse executeMqLock(@RequestBody @Validated KillDto dto, BindingResult result, HttpSession session) {
        if (result.hasErrors() || dto.getKillId() <= 0) {
            return new BaseResponse(StatusCode.INVALID_PARAMS);
        }
        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        Map<String, Object> dataMap = Maps.newHashMap();
        try {
            //下面数据用网页登录测试用
//            Object uId=session.getAttribute("uid");
//            Integer userId= (Integer)uId ;
//            dto.setUserId(userId);
            //下面数据用jmeter压测用
            dataMap.put("killId", dto.getKillId());
            dataMap.put("userId", dto.getUserId());
//            dataMap.put("userId",userId);
            response.setData(dataMap);
            // TODO response.setData(dataMap); 返回数据，异步请求mq
            // 去掉redis的if判断是因为killItemV4里走数据库验证判断是否秒杀过了。如果优化不走数据库可以释放此步骤
//            if (checkService.checkSeckillUser(dto,dto.getUserId())) {
            //信息保存redis成功之后 同步向mq发送消息队列
            for (int i = 0; i < 2; i++) {
                rabbitSenderService.sendKillExecuteMqMsg(dto);
            }

//            }else {
//                response.setCode(StatusCode.FAIL.getCode());
//            }
//            response.setMsg( "您已经秒杀过该商品，不能再次秒杀" );
        } catch (Exception e) {
            response = new BaseResponse(StatusCode.FAIL.getCode(), e.getMessage());
        }
        return response;
    }
}








































