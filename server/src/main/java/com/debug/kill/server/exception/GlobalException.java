package com.debug.kill.server.exception;

import com.debug.kill.api.enums.StatusCode;

/**
 *
 * @author:ZHAOYONGHENG
 * @date:2021/12/2
 * @version:1.0.0
 */
public class GlobalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private StatusCode statusCode;

    public GlobalException(StatusCode statusCode) {
        super(statusCode.toString());
        this.statusCode = statusCode;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(StatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
