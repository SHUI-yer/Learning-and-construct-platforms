package com.learning.lpcommon.exception;

/*
* 账号不存在
* */
public class NotFoundUserException extends BaseException{
    public NotFoundUserException() {
    }

    public NotFoundUserException(String msg) {
        super(msg);
    }
}
