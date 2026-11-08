package com.learning.lpcommon.validation;

import jakarta.validation.groups.Default;

/** 共用 DTO 在不同操作下的附加校验组。 */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    /** 更新操作会同时执行 Default 组中的通用字段校验。 */
    public interface Update extends Default {
    }
}
