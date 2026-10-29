package com.learning.learning.service;

import com.learning.learning.dto.DocProgressDTO;
import com.learning.learning.vo.DocProgressVO;

public interface DocProgressService {

    void reportDocProgress(DocProgressDTO dto, Long userId);

    DocProgressVO getDocProgress(Long resourceId, Long userId);

    void batchFlushToDB();
}
