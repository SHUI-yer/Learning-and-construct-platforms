package com.learning.lpcommon.exception;

import com.learning.lpcommon.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器：将异常统一转换为 Result 结构，
 * 避免 Spring 默认错误体（HTTP 500 + 堆栈）导致前端拿不到业务文案
 *
 * 放在 lp-common 而非某个业务模块：只有公共服务被所有模块依赖、且落在
 * LpStarterApplication 的 com.learning 扫描范围内，放在业务模块里会出现
 * 「模块没被 starter 依赖 → 处理器从未注册」的情况（历史踩坑）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：msg 就是给用户看的文案，必须原样传给前端
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * @RequestBody + @Valid 参数校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = extractMessage(e.getBindingResult().getFieldError());
        log.warn("参数校验失败: {}", msg);
        return Result.error(msg);
    }

    /**
     * 表单/Query 参数绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = extractMessage(e.getBindingResult().getFieldError());
        log.warn("参数绑定失败: {}", msg);
        return Result.error(msg);
    }

    /**
     * 兜底异常处理，保证响应结构统一
     * 注意：此处应答体不含堆栈，排查请查后端日志（log.error 已打印完整堆栈）
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }

    private String extractMessage(FieldError fieldError) {
        if (fieldError == null) {
            return "参数校验失败";
        }
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
