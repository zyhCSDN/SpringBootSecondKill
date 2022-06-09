package com.debug.kill.server.exception;

import com.debug.kill.api.enums.StatusCode;
import com.debug.kill.api.response.BaseResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 全局异常
 *
 * @author:ZHAOYONGHENG
 * @date:2021/12/2
 * @version:1.0.0
 */

//@ControllerAdvice // 使用 @ControllerAdvice 实现全局异常处理
//@ResponseBody // @ResponseBody的作用其实是将java对象转为json格式的数据。
//@RestControllerAdvice作用等同于@ResponseBody加上@ControllerAdvice,会在所有带有@Controller或者@RestController注解的类上生效,还可以使用basePackages参数配置指定异常处理类生效的包

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public BaseResponse exceptionHandler(HttpServletRequest request, Exception e) {
        e.printStackTrace();
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            return new BaseResponse(ex.getStatusCode());
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            List<ObjectError> errors = ex.getAllErrors();
            ObjectError error = errors.get(0);
            String msg = error.getDefaultMessage();
            return new BaseResponse(StatusCode.SERVER_ERROR.getCode(), msg);
        } else {
            return new BaseResponse(StatusCode.SERVER_ERROR);
        }
    }
}
