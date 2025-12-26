package org.example.common.api;

import lombok.Data;
import org.example.common.exception.ErrorCode;

import java.io.Serializable;

/**
 * 统一 API 响应结果封装
 * @param <T> 承载的数据类型
 */
@Data
public class Result<T> implements Serializable {

    private String code;    // 状态码 (如 "0000" 表示成功, "B001" 表示业务异常)
    private String msg;     // 描述信息
    private T data;         // 承载的数据

    // 私有构造，强制使用静态工厂方法
    private Result() {}

    /**
     * 成功返回
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 成功返回（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCode.SUCCESS.getCode());
        result.setMsg(ErrorCode.SUCCESS.getMsgKey()); // 这里存的是 Key "response.success"
        result.setData(data);
        return result;
    }

    /**
     * 失败返回
     */
    public static <T> Result<T> failure(String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}