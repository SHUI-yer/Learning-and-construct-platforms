package com.learning.lpcommon.exception;

/** 资源版本或当前状态与本次操作冲突。 */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(message);
    }
}
