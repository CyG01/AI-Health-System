package com.example.common;

import java.io.Serializable;

public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 失败状态码 */
    public static final int FAIL_CODE = 500;

    /** 未授权状态码 */
    public static final int UNAUTHORIZED_CODE = 401;

    /** Token过期状态码 - access token过期 */
    public static final int TOKEN_EXPIRED_CODE = 40101;

    /** Token过期状态码 - refresh token过期 */
    public static final int REFRESH_TOKEN_EXPIRED_CODE = 40102;

    /** 无权限状态码 */
    public static final int FORBIDDEN_CODE = 403;

    /** 资源不存在状态码 */
    public static final int NOT_FOUND_CODE = 404;

    /** 请求参数错误状态码 */
    public static final int BAD_REQUEST_CODE = 400;

    private Integer code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "操作成功", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(SUCCESS_CODE, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(FAIL_CODE, msg, null);
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> badRequest(String msg) {
        return new Result<>(BAD_REQUEST_CODE, msg, null);
    }

    public static <T> Result<T> unauthorized() {
        return new Result<>(UNAUTHORIZED_CODE, "未登录或token已过期", null);
    }

    public static <T> Result<T> unauthorized(String msg) {
        return new Result<>(UNAUTHORIZED_CODE, msg, null);
    }

    /**
     * Token过期 - access token过期，可刷新
     */
    public static <T> Result<T> tokenExpired() {
        return new Result<>(TOKEN_EXPIRED_CODE, "token已过期，请刷新token", null);
    }

    /**
     * Token过期 - refresh token也过期了，需要重新登录
     */
    public static <T> Result<T> refreshTokenExpired() {
        return new Result<>(REFRESH_TOKEN_EXPIRED_CODE, "登录已过期，请重新登录", null);
    }

    public static <T> Result<T> forbidden() {
        return new Result<>(FORBIDDEN_CODE, "无权限访问", null);
    }

    public static <T> Result<T> forbidden(String msg) {
        return new Result<>(FORBIDDEN_CODE, msg, null);
    }

    public static <T> Result<T> notFound() {
        return new Result<>(NOT_FOUND_CODE, "资源不存在", null);
    }

    public static <T> Result<T> notFound(String msg) {
        return new Result<>(NOT_FOUND_CODE, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == SUCCESS_CODE;
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
