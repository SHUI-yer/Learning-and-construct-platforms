package com.learning.assessment.service;

import com.learning.assessment.dto.GradeDesignDTO;
import com.learning.assessment.dto.TaskCreateDTO;
import com.learning.assessment.vo.GradeDesignVO;
import com.learning.assessment.vo.TaskVO;

import java.util.List;

public interface TaskService {

    List<GradeDesignVO> saveGradeDesign(GradeDesignDTO dto);

    List<GradeDesignVO> getGradeDesign(Long courseId);

    TaskVO createTask(TaskCreateDTO dto);

    TaskVO updateTask(TaskCreateDTO dto);

    void deleteTask(Long taskId);

    TaskVO getTaskById(Long taskId);

    List<TaskVO> listTasksByCourse(Long courseId);

    List<TaskVO> listTasksByConditions(Long courseId, String taskType, String keyword);
}