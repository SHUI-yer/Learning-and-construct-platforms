package com.learning.assessment.controller;

import com.learning.assessment.dto.GradeDesignDTO;
import com.learning.assessment.dto.TaskCreateDTO;
import com.learning.assessment.service.TaskService;
import com.learning.assessment.vo.GradeDesignVO;
import com.learning.assessment.vo.TaskVO;
import com.learning.lpcommon.result.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Resource
    private TaskService taskService;

    // ========== 成绩构成 ==========

    @PostMapping("/gradeDesign/save")
    public Result<List<GradeDesignVO>> saveGradeDesign(@RequestBody GradeDesignDTO gradeDesignDTO) {
        List<GradeDesignVO> result = taskService.saveGradeDesign(gradeDesignDTO);
        return Result.success(result);
    }

    @GetMapping("/gradeDesign/{courseId}")
    public Result<List<GradeDesignVO>> getGradeDesign(@PathVariable Long courseId) {
        List<GradeDesignVO> result = taskService.getGradeDesign(courseId);
        return Result.success(result);
    }

    // ========== 任务管理 ==========

    @PostMapping("/create")
    public Result<TaskVO> createTask(@RequestBody TaskCreateDTO dto) {
        TaskVO result = taskService.createTask(dto);
        return Result.success(result);
    }

    @PutMapping("/update")
    public Result<TaskVO> updateTask(@RequestBody TaskCreateDTO dto) {
        TaskVO result = taskService.updateTask(dto);
        return Result.success(result);
    }

    @DeleteMapping("/{taskId}")
    public Result<String> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return Result.success("删除成功");
    }

    @GetMapping("/{taskId}")
    public Result<TaskVO> getTaskById(@PathVariable Long taskId) {
        TaskVO result = taskService.getTaskById(taskId);
        return Result.success(result);
    }

    @GetMapping("/list/{courseId}")
    public Result<List<TaskVO>> listTasksByCourse(@PathVariable Long courseId) {
        List<TaskVO> result = taskService.listTasksByCourse(courseId);
        return Result.success(result);
    }

    @GetMapping("/search")
    public Result<List<TaskVO>> searchTasks(@RequestParam Long courseId,
                                            @RequestParam(required = false) String taskType,
                                            @RequestParam(required = false) String keyword) {
        List<TaskVO> result = taskService.listTasksByConditions(courseId, taskType, keyword);
        return Result.success(result);
    }
}