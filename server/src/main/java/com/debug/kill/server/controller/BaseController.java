package com.debug.kill.server.controller;/**
 * Created by Administrator on 2019/6/13.
 */

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.model.mapper.RandomCodeMapper;
import com.debug.kill.server.thread.CodeGenerateSnowThread;
import com.debug.kill.server.thread.CodeGenerateThread;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 基础controller
 * @Author:debug (SteadyJack)
 * @Date: 2019/6/13 23:36
 **/
@Controller
@RequestMapping("base")
@Api(tags = "基本BaseController")
public class BaseController {

    private static final Logger log= LoggerFactory.getLogger(BaseController.class);

    /**
     * 跳转页面
     * @param name
     * @param modelMap
     * @return
     */
    @GetMapping("welcomer")
    public String welcomer(String name, ModelAndView modelMap){
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        modelMap.addObject("name",name);
        return "welcome";
    }

    @GetMapping("index")
    public String index(){
        return "login";
    }

    /**
     * 前端发起请求获取数据
     * @param name
     * @return
     */
    @RequestMapping(value = "/data",method = RequestMethod.GET)
    @ApiOperation(value = "接口名", notes = "接口描述", httpMethod = "POST")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "length",value = "参数1", required = false, paramType = "path"),
            @ApiImplicitParam(name = "size",value = "参数2", required = true, paramType = "query"),
            @ApiImplicitParam(name = "page",value = "参数3", required = true, paramType = "header"),
            @ApiImplicitParam(name = "total",value = "参数4", required = true, paramType = "form"),
            @ApiImplicitParam(name = "start",value = "参数5",dataType = "string", paramType = "body")
    })
    @ResponseBody
    public String data(String name){
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        return name;
    }

    /**
     * 标准请求-响应数据格式
     * @param name
     * @return
     */
    @RequestMapping(value = "/response",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse response(String name){
        BaseResponse response=new BaseResponse(StatusCode.SUCCESS);
        if (StringUtils.isBlank(name)){
            name="这是welcome!";
        }
        response.setData(name);
        return response;
    }

    @RequestMapping(value = "/error",method = RequestMethod.GET)
    public String error(){
        return "error";
    }


    @Autowired
    private RandomCodeMapper randomCodeMapper;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 测试在高并发下多线程生成订单编号-传统的随机数生成方法
     * @return
     */
    @RequestMapping(value = "/code/generate/thread",method = RequestMethod.GET)
    public BaseResponse codeThread(){
        BaseResponse response=new BaseResponse(StatusCode.SUCCESS);
        try {
            ExecutorService executorService=Executors.newFixedThreadPool(10);
            for (int i=0;i<1000;i++){
                executorService.execute(new CodeGenerateThread(randomCodeMapper));
            }
        }catch (Exception e){
            response=new BaseResponse(StatusCode.FAIL.getCode(),e.getMessage());
        }
        return response;
    }


    /**
     * 测试在高并发下多线程生成订单编号-雪花算法
     * @return
     */
    @RequestMapping(value = "/code/generate/thread/snow",method = RequestMethod.GET)
    @ResponseBody
    public BaseResponse codeThreadSnowFlake(){
        BaseResponse response=new BaseResponse(StatusCode.SUCCESS);
        try {
            ExecutorService executorService=Executors.newFixedThreadPool(8);
            for (int i=0;i<1000;i++){
//                log.info("执行的线程名称name1:{}",Thread.currentThread().getName());
                executorService.execute(new CodeGenerateSnowThread(randomCodeMapper,redisTemplate));
            }
            response.setData("十万数据插入成功");
        }catch (Exception e){
            response=new BaseResponse(StatusCode.FAIL.getCode(),e.getMessage());
        }
        return response;
    }
}























