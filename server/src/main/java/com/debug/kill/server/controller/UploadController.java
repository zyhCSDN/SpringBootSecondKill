package com.debug.kill.server.controller;

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import com.debug.kill.server.exception.GlobalException;
import com.debug.kill.server.utils.COSClientInit;
import com.qcloud.cos.transfer.Upload;
import io.swagger.annotations.Api;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * 文件上传
 *
 * @author:ZHAOYONGHENG
 * @date:2021/12/2
 * @version:1.0.0
 */
@RestController
@RequestMapping("/upload")
@Api(tags = "图片上传")
public class UploadController {

    public static final String LOCALPATH = "D:/data/file/";

    /**
     * 上传用户图片
     *
     * @param file
     * @return
     * @author sunran
     */
    @ResponseBody
    @RequestMapping("/uploadV1")
    public BaseResponse uploadPicture(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        BaseResponse response = new BaseResponse(StatusCode.SUCCESS);
        if (file == null) {
            throw new GlobalException(StatusCode.INVALID_PARAMS);
        }
        //上传最大为1MB
        int maxSize = 1024 * 1024 * 1;
        if (file.getSize() > maxSize) {
            response.setMsg("最大上传限制1Mb");
            return response;
        }
        //获取文件名加后缀
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName != "") {
            //文件后缀
            String fileF = fileName.substring(fileName.lastIndexOf("."));
            if (!(fileF.equals(".jpg") || fileF.equals(".jpeg") || fileF.equals(".png") || fileF.equals(".image"))) {
                response.setMsg("只能上传jpg,jpeg,png,image格式");
                return response;
            }
            //新的文件名
            fileName = System.currentTimeMillis() + "_" + new Random().nextInt(1000) + fileF;
            //以流形式上传 以文件形式必须本地有文件
            InputStream inputStream = file.getInputStream();
            try {
                String url = COSClientInit.uploadFileV1(inputStream, "images/" + fileName);
                System.out.println("图片上传成功 url:" + url);
                response.setData(url);
            } catch (Exception e) {
                e.printStackTrace();
                response.setMsg("系统异常，图片上传失败");
            }
        }
        return response;
    }
}
