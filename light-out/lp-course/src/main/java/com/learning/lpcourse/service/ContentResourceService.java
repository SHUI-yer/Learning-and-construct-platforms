package com.learning.lpcourse.service;

import com.learning.lpcourse.dto.ContentResourceDTO;
import com.learning.lpcommon.result.Result;

public interface ContentResourceService {
    Result<Void> addResource(ContentResourceDTO dto);
    Result<Void> updateResource(ContentResourceDTO dto);
    Result<Void> deleteResource(Long resourceId);
}