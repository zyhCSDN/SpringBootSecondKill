package com.debug.kill.server.controller;

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.server.utils.QRCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author ZhaoYongHeng
 * @date 2020/12/8
 */
@Controller
@RequestMapping("qr/code")
@Slf4j
public class QrCodeController {


    private static final String RootPath="E:\\shFiles\\QRCode";
    private static final String FileFormat=".png";

    private static final ThreadLocal< SimpleDateFormat > LOCALDATEFORMAT=ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmss"));

    //生成二维码并将其存放于本地目录
    @PostMapping("generate/v1")
    @ResponseBody
    public BaseResponse generateV1(String content){
        BaseResponse response=new BaseResponse(StatusCode.Success);
        try {
            final String fileName=LOCALDATEFORMAT.get().format(new Date());
            QRCodeUtil.createCodeToFile(content,new File(RootPath),fileName+FileFormat);
        }catch (Exception e){
            response=new BaseResponse(StatusCode.Fail.getCode(),e.getMessage());
        }
        return response;
    }

    //生成二维码并将其返回给前端调用者
    @PostMapping("generate/v2")
    @ResponseBody
    public BaseResponse generateV2(String content, HttpServletResponse servletResponse){
        BaseResponse response=new BaseResponse(StatusCode.Success);
        content="11111110";
        try {
            QRCodeUtil.createCodeToOutputStream(content,servletResponse.getOutputStream());
        }catch (Exception e){
           new Throwable(e.getMessage());
        }
        return response;
    }

    //生成二维码并将其返回给前端调用者
    @RequestMapping("generate/v3/{content}")
    public void generateV3(@PathVariable String content, HttpServletResponse servletResponse){
        try {
            QRCodeUtil.createCodeToOutputStream(content,servletResponse.getOutputStream());
        }catch (Exception e){
            new Throwable(e.getMessage());
        }
    }
}
