package com.learning.assessment.service.impl;

import com.learning.assessment.entity.GradeSummary;
import com.learning.assessment.mapper.GradeMapper;
import com.learning.assessment.mapper.GradeSummaryMapper;
import com.learning.assessment.service.GradeSummaryService;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.GradeSummaryVO;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GradeSummaryServiceImpl implements GradeSummaryService {

    @Resource
    private GradeSummaryMapper gradeSummaryMapper;
    @Resource
    private GradeMapper gradeMapper;
    @Resource
    private AssessmentAccessGuard accessGuard;

    // ========== 汇总 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<GradeSummaryVO> summarize(Long courseId) {
        validateCourseId(courseId);
        accessGuard.requireTeaching("汇总成绩");
        accessGuard.requireCourseTeacher(courseId, "汇总成绩");
        List<Long> studentIds = gradeMapper.selectStudentIdsByCourseId(courseId);
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BaseException("该课程暂无成绩记录，无法汇总");
        }
        for (Long studentId : studentIds) {
            BigDecimal finalScore = scale2(gradeMapper.sumWeightedScoreByCourseAndStudent(courseId, studentId));
            GradeSummary summary = gradeSummaryMapper.selectByCourseAndStudent(courseId, studentId);
            if (summary == null) {
                summary = new GradeSummary();
                summary.setCourseId(courseId);
                summary.setStudentId(studentId);
                summary.setFinalScore(finalScore);
                summary.setIsExported(0);
                gradeSummaryMapper.insert(summary);
            } else if (summary.getConfirmedAt() == null) {
                // 已确认记录不覆盖（改分走成绩复核），仅更新未确认记录
                summary.setFinalScore(finalScore);
                int rows = gradeSummaryMapper.update(summary);
                if (rows == 0) {
                    throw new BaseException("成绩汇总并发冲突，请重试");
                }
            }
        }
        return orderedList(courseId);
    }

    // ========== 列表 ==========

    @Override
    public PageResult<GradeSummaryVO> listByCourse(Long courseId, Integer page, Integer pageSize) {
        validateCourseId(courseId);
        // 学生只能看到自己的汇总，不再把全班成绩与排名整页下发到学生浏览器
        if (!accessGuard.isTeaching()) {
            Long studentId = accessGuard.requireUserId("查看成绩");
            GradeSummary own = gradeSummaryMapper.selectByCourseAndStudent(courseId, studentId);
            List<GradeSummaryVO> records = own == null
                    ? List.of()
                    : List.of(GradeSummaryVO.fromEntity(own));
            return PageResult.build(records, (long) records.size(), 1L, 10L);
        }
        accessGuard.requireCourseTeacher(courseId, "查看成绩");
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        long total = gradeSummaryMapper.countByCourseId(courseId);
        // 已确认按排名升序，未确认按总分降序
        List<GradeSummary> list = isConfirmed(courseId)
                ? gradeSummaryMapper.selectPageRankedByCourseId(courseId, (p - 1) * ps, ps)
                : gradeSummaryMapper.selectPageByCourseId(courseId, (p - 1) * ps, ps);
        List<GradeSummaryVO> records = list.stream().map(GradeSummaryVO::fromEntity).toList();
        return PageResult.build(records, total, (long) p, (long) ps);
    }

    // ========== 确认 + 排名 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<GradeSummaryVO> confirm(Long courseId) {
        validateCourseId(courseId);
        accessGuard.requireTeaching("确认成绩");
        accessGuard.requireCourseTeacher(courseId, "确认成绩");
        List<GradeSummary> all = gradeSummaryMapper.selectByCourseId(courseId);
        if (all.isEmpty()) {
            throw new BaseException("该课程暂无汇总记录，请先执行成绩汇总");
        }
        if (all.stream().allMatch(s -> s.getConfirmedAt() != null)) {
            return orderedList(courseId);
        }

        Long teacherId = currentUserId();
        LocalDateTime now = LocalDateTime.now();
        // 同分并列同名次：final_score DESC；名次跳跃（85/85/80 -> 1/1/3）
        int rank = 0;
        int prevRank = 0;
        BigDecimal prevScore = null;
        for (GradeSummary s : all) {
            rank++;
            if (prevScore != null && s.getFinalScore() != null && s.getFinalScore().compareTo(prevScore) == 0) {
                s.setRankInCourse(prevRank);
            } else {
                s.setRankInCourse(rank);
            }
            s.setConfirmedBy(teacherId);
            s.setConfirmedAt(now);
            int rows = gradeSummaryMapper.update(s);
            if (rows == 0) {
                throw new BaseException("成绩确认并发冲突，请重试");
            }
            prevScore = s.getFinalScore();
            prevRank = s.getRankInCourse();
        }
        return orderedList(courseId);
    }

    // ========== 导出 Excel ==========

    @Override
    public void exportExcel(Long courseId, HttpServletResponse response) {
        validateCourseId(courseId);
        accessGuard.requireTeaching("导出成绩单");
        accessGuard.requireCourseTeacher(courseId, "导出成绩单");
        List<GradeSummary> list = orderedEntities(courseId);
        if (list.isEmpty()) {
            throw new BaseException("该课程暂无汇总记录，请先执行成绩汇总");
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("成绩单");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("排名");
            header.createCell(1).setCellValue("学生ID");
            header.createCell(2).setCellValue("姓名");
            header.createCell(3).setCellValue("总分");

            int rowIdx = 1;
            for (GradeSummary s : list) {
                Row row = sheet.createRow(rowIdx);
                // 未确认时无排名，按总分降序导出并取行序号占位
                row.createCell(0).setCellValue(s.getRankInCourse() == null ? rowIdx : s.getRankInCourse());
                row.createCell(1).setCellValue(s.getStudentId() == null ? "" : s.getStudentId().toString());
                // TODO 用户体系打通后补充学生姓名
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue(s.getFinalScore() == null ? 0 : s.getFinalScore().doubleValue());
                rowIdx++;
            }
            sheet.setColumnWidth(0, 8 * 256);
            sheet.setColumnWidth(1, 16 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 12 * 256);

            String filename = URLEncoder.encode("成绩单-" + courseId + ".xlsx", StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new BaseException("成绩单导出失败：" + e.getMessage());
        }
        markExported(courseId);
    }

    private void markExported(Long courseId) {
        List<GradeSummary> list = gradeSummaryMapper.selectByCourseId(courseId);
        for (GradeSummary s : list) {
            if (s.getIsExported() == null || s.getIsExported() == 0) {
                s.setIsExported(1);
                int rows = gradeSummaryMapper.update(s);
                if (rows == 0) {
                    throw new BaseException("成绩单导出标记失败，请重试");
                }
            }
        }
    }

    private List<GradeSummaryVO> orderedList(Long courseId) {
        return orderedEntities(courseId).stream().map(GradeSummaryVO::fromEntity).toList();
    }

    private List<GradeSummary> orderedEntities(Long courseId) {
        return isConfirmed(courseId)
                ? gradeSummaryMapper.selectRankedByCourseId(courseId)
                : gradeSummaryMapper.selectByCourseId(courseId);
    }

    private boolean isConfirmed(Long courseId) {
        return gradeSummaryMapper.countConfirmedByCourseId(courseId) > 0;
    }

    private void validateCourseId(Long courseId) {
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
    }

    private BigDecimal scale2(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // 不再用 0 占位：confirmed_by 指向不存在的用户会触发外键错误或留下孤儿数据
            throw new com.learning.lpcommon.exception.ForbiddenException("未登录或登录状态无效");
        }
        return userId;
    }
}
