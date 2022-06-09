package com.debug.kill.api.enums;

/**
 * 通用状态码
 */
public enum StatusCode {
    SUCCESS(200, "成功"),
    FAIL(-1, "失败"),
    DELETED(1, "已删除"),
    NOT_DELETED(0, "未删除"),
    ENABLED(1, "启用"),
    NOT_ENABLE(0, "未启用"),
    SEX_MAN(1, "男"),
    SEX_WOMAN(2, "女"),
    INVALID_PARAMS(201, "非法的参数!"),
    USER_NOT_LOGIN(202, "用户没登录"),
    ADD_SUCCESS(1000, "新增成功！"),
    ADD_FAIL(1001, "新增失败！"),
    MODIFY_SUCCESS(1100, "修改成功！"),
    MODIFY_FAIL(1101, "修改失败！"),
    REMOVE_SUCCESS(1200, "删除成功！"),
    REMOVE_FAIL(1201, "删除失败！"),
    LOGIN_FAIL(1301, "登录失败！用户名密码错误！"),
    SESSION_TIMEOUT(1302, "session超时，请重新登录！"),
    NO_AUTH(1303, "没有权限访问请求资源，请切换账户后重试！"),
    USERNAME_EXISTS(1401, "用户名已存在！"),
    GROUPNAME_EXISTS(1402, "用户组名已存在！"),
    SUB_MENU_EXISTS(1403, "菜单下还存在子菜单！"),
    ASSIGN_SUCCESS(1500, "分配成功！"),
    ASSIGN_FAIL(1501, "分配失败！"),
    ORDER_SUCCESS(1600, "排序成功！"),
    ORDER_FAIL(1601, "排序失败！"),
    SERVER_ERROR(500, "服务异常！");

    private Integer code;
    private String msg;

    StatusCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
