package com.learning.assessment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.assessment.dto.QuestionCreateDTO;
import com.learning.assessment.dto.QuestionImportRow;
import com.learning.assessment.entity.QuestionBank;
import com.learning.assessment.entity.QuestionImport;
import com.learning.assessment.mapper.QuestionBankMapper;
import com.learning.assessment.mapper.QuestionImportMapper;
import com.learning.assessment.service.QuestionImportService;
import com.learning.assessment.service.QuestionValidator;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.util.QuestionFileParser;
import com.learning.assessment.vo.ImportErrorVO;
import com.learning.assessment.vo.QuestionImportVO;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class QuestionImportServiceImpl implements QuestionImportService {

    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> SUPPORTED_EXT = Set.of(".xlsx", ".xls", ".docx");
    private static final String STATUS_PROCESSING = "processing";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";

    @Resource
    private QuestionImportMapper questionImportMapper;
    @Resource
    private QuestionBankMapper questionBankMapper;
    @Resource
    private QuestionFileParser questionFileParser;
    @Resource
    private QuestionValidator questionValidator;
    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private AssessmentAccessGuard accessGuard;

    @Value("${assessment.upload-dir:uploads/question_import}")
    private String uploadDir;

    @Override
    @Transactional(rollbackFor = Exception.class, noRollbackFor = BaseException.class)
    public QuestionImportVO importQuestions(MultipartFile file, Long courseId) {
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        accessGuard.requireTeaching("导入题目");
        accessGuard.requireCourseTeacher(courseId, "导入题目");
        if (file == null || file.isEmpty()) {
            throw new BaseException("导入文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!SUPPORTED_EXT.contains(ext)) {
            throw new BaseException("仅支持 .xlsx/.xls/.docx 格式文件");
        }

        Long operatorId = currentUserId();

        QuestionImport record = new QuestionImport();
        record.setCourseId(courseId);
        record.setOperatorId(operatorId);
        record.setFileFormat("word".equals(ext) ? "word" : "excel");
        record.setStatus(STATUS_PROCESSING);
        try {
            record.setFileUrl(saveFile(file, ext));
        } catch (IOException e) {
            throw new BaseException("文件保存失败，请重试");
        }
        questionImportMapper.insert(record);

        List<QuestionImportRow> rows;
        try {
            rows = questionFileParser.parse(file.getBytes(), originalName);
        } catch (IOException e) {
            updateFailed(record, "文件读取失败，请重试");
            throw new BaseException("文件读取失败，请重试");
        } catch (BaseException e) {
            updateFailed(record, e.getMessage());
            throw e;
        }

        List<ImportErrorVO> errors = new ArrayList<>();
        int success = 0;
        for (QuestionImportRow row : rows) {
            try {
                QuestionCreateDTO dto = buildDto(row, courseId, operatorId);
                questionValidator.validate(dto);
                QuestionBank q = new QuestionBank();
                q.setCourseId(courseId);
                q.setCreatorId(operatorId);
                q.setQType(dto.getQType());
                q.setContent(dto.getContent());
                q.setOptions(QuestionValidator.toJsonArray(dto.getOptions()));
                q.setAnswer(dto.getAnswer());
                q.setAnalysis(dto.getAnalysis());
                q.setDifficulty(dto.getDifficulty() == null ? 2 : dto.getDifficulty());
                q.setTags(QuestionValidator.toJsonArray(dto.getTags()));
                q.setSource("batch_import");
                q.setImportId(record.getId());
                questionBankMapper.insert(q);
                success++;
            } catch (BaseException e) {
                errors.add(ImportErrorVO.builder().rowNo(row.getRowNo()).reason(e.getMessage()).build());
            }
        }

        record.setImportCount(rows.size());
        record.setSuccessCount(success);
        record.setFailCount(errors.size());
        record.setErrorDetail(errors.isEmpty() ? null : toJson(errors));
        record.setStatus(STATUS_COMPLETED);
        questionImportMapper.update(record);
        return QuestionImportVO.fromEntity(questionImportMapper.selectById(record.getId()));
    }

    @Override
    public PageResult listImports(Long courseId, Integer page, Integer pageSize) {
        accessGuard.requireTeaching("查看导入记录");
        accessGuard.requireCourseTeacher(courseId, "查看导入记录");
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        long total = questionImportMapper.countByCourseId(courseId);
        List<QuestionImport> list = questionImportMapper.selectByCourseIdPage(courseId, (p - 1) * ps, ps);
        List<QuestionImportVO> records = list.stream()
                .map(q -> QuestionImportVO.fromEntity(q, false))
                .toList();
        return PageResult.build(records, total, (long) p, (long) ps);
    }

    @Override
    public QuestionImportVO getImportDetail(Long id) {
        accessGuard.requireTeaching("查看导入详情");
        QuestionImport record = questionImportMapper.selectById(id);
        if (record == null) {
            throw new BaseException("导入记录不存在");
        }
        return QuestionImportVO.fromEntity(record);
    }

    // ========== 内部方法 ==========

    private QuestionCreateDTO buildDto(QuestionImportRow row, Long courseId, Long operatorId) {
        if (row.getError() != null) {
            throw new BaseException(row.getError());
        }
        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setCourseId(courseId);
        String qType = questionValidator.normalizeQType(row.getQType());
        if (qType == null) {
            throw new BaseException("题型不合法: " + row.getQType());
        }
        dto.setQType(qType);
        dto.setContent(row.getContent());
        dto.setOptions(row.getOptions());
        dto.setAnswer(normalizeAnswer(qType, row.getAnswer()));
        dto.setAnalysis(row.getAnalysis());
        dto.setDifficulty(row.getDifficulty());
        dto.setTags(row.getTags());
        return dto;
    }

    /**
     * 判断题答案兼容中文；选择题答案兼容字母(A/B/C/D)，统一转为下标。
     */
    private String normalizeAnswer(String qType, String answer) {
        if (answer == null) {
            return null;
        }
        String a = answer.trim();
        if (a.isEmpty()) {
            return a;
        }
        return switch (qType) {
            case "judge" -> switch (a) {
                case "正确", "对", "是", "√", "T", "true" -> "1";
                case "错误", "错", "否", "×", "F", "false" -> "0";
                default -> a;
            };
            case "single_choice", "multiple_choice" -> convertChoiceAnswer(a);
            default -> a;
        };
    }

    private String convertChoiceAnswer(String answer) {
        List<String> parts = new ArrayList<>();
        for (String p : answer.split("[,，;；、]")) {
            String item = p.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (item.matches("[A-Da-d]")) {
                parts.add(String.valueOf(item.toUpperCase(Locale.ROOT).charAt(0) - 'A'));
            } else {
                parts.add(item);
            }
        }
        return String.join(",", parts);
    }

    private String extensionOf(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx < 0 ? "" : filename.substring(idx).toLowerCase(Locale.ROOT);
    }

    private String saveFile(MultipartFile file, String ext) throws IOException {
        String dateDir = LocalDate.now().format(DATE_DIR);
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadDir, dateDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        file.transferTo(target.toFile());
        return target.toString().replace("\\", "/");
    }

    private void updateFailed(QuestionImport record, String reason) {
        record.setImportCount(0);
        record.setSuccessCount(0);
        record.setFailCount(0);
        record.setErrorDetail(toJson(List.of(ImportErrorVO.builder().reason(reason).build())));
        record.setStatus(STATUS_FAILED);
        questionImportMapper.update(record);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            // operator_id 是外键，0 不是有效用户，会直接导致导入记录写入失败
            throw new com.learning.lpcommon.exception.ForbiddenException("未登录或登录状态无效");
        }
        return userId;
    }
}
