package com.debug.kill.server.utils;

import com.qcloud.cos.http.CosHttpRequest;
import com.qcloud.cos.internal.CosServiceRequest;
import com.qcloud.cos.retry.RetryPolicy;
import org.apache.http.HttpResponse;

import java.io.IOException;

/**
 * @author:ZHAOYONGHENG
 * @date:2021/12/6
 * @version:1.0.0
 */
// 自定义重试策略
public class OnlyIOExceptionRetryPolicy extends RetryPolicy {
    @Override
    public <X extends CosServiceRequest> boolean shouldRetry(CosHttpRequest<X> request,
                                                             HttpResponse response,
                                                             Exception exception,
                                                             int retryIndex) {
        // 如果是客户端的 IOException 异常则重试，否则不重试
        if (exception.getCause() instanceof IOException) {
            return true;
        }
        return false;
    }
}
