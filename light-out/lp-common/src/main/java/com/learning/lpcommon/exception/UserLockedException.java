package com.learning.lpcommon.exception;

/*
* 账号被禁用
* */
public class UserLockedException extends BaseException{
    public UserLockedException() {
    }

    public UserLockedException(String msg) {
        super(msg);
    }
}
