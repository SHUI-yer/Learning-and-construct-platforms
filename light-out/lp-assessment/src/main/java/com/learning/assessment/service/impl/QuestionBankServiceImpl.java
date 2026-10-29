package com.learning.assessment.service.impl;

import com.learning.assessment.dto.QuestionCreateDTO;
import com.learning.assessment.entity.QuestionBank;
import com.learning.assessment.mapper.QuestionBankMapper;
import com.learning.assessment.service.QuestionBankService;
import com.learning.assessment.service.QuestionValidator;
import com.learning.assessment.support.AssessmentAccessGuard;
import com.learning.assessment.vo.QuestionVO;
import com.learning.lpcommon.exception.BaseException;
import com.learning.lpcommon.result.PageResult;
import com.learning.lpcommon.utils.UserContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    @Resource
    private QuestionBankMapper questionBankMapper;

    @Resource
    private QuestionValidator questionValidator;

    @Resource
    private AssessmentAccessGuard accessGuard;

    @Override
    public QuestionVO createQuestion(QuestionCreateDTO dto) {
        accessGuard.requireTeaching("新增题目");
        accessGuard.requireCourseTeacher(dto == null ? null : dto.getCourseId(), "新增题目");
        questionValidator.validate(dto);
        QuestionBank q = buildEntity(dto);
        // 用户体系已接入：此前用 0 占位会导致外键 fk_question_creator 失败
        Long creatorId = UserContext.getUserId();
        if (creatorId == null) {
            throw new BaseException("未获取到登录用户，请重新登录");
        }
        q.setCreatorId(creatorId);
        q.setSource("manual");
        questionBankMapper.insert(q);
        // insert 语句固定写入 version=0，但 MyBatis 不回填，此处对齐返回值免得前端拿到 null 再回传
        q.setVersion(0);
        return QuestionVO.fromEntity(q);
    }

    @Override
    public QuestionVO updateQuestion(QuestionCreateDTO dto) {
        if (dto.getQId() == null) {
            throw new BaseException("题目ID不能为空");
        }
        QuestionBank existing = questionBankMapper.selectById(dto.getQId());
        if (existing == null) {
            throw new BaseException("题目不存在");
        }
        accessGuard.requireTeaching("修改题目");
        accessGuard.requireCourseTeacher(existing.getCourseId(), "修改题目");
        if (dto.getVersion() == null) {
            throw new BaseException("编辑题目必须携带乐观锁版本号");
        }
        questionValidator.validate(dto);

        QuestionBank q = buildEntity(dto);
        q.setQId(dto.getQId());
        q.setVersion(dto.getVersion());

        int rows = questionBankMapper.update(q);
        if (rows == 0) {
            throw new BaseException("题目已被他人修改，请刷新后重试");
        }
        return QuestionVO.fromEntity(questionBankMapper.selectById(dto.getQId()));
    }

    @Override
    public void deleteQuestion(Long qId) {
        QuestionBank existing = questionBankMapper.selectById(qId);
        if (existing == null) {
            throw new BaseException("题目不存在");
        }
        accessGuard.requireTeaching("删除题目");
        accessGuard.requireCourseTeacher(existing.getCourseId(), "删除题目");
        questionBankMapper.deleteById(qId);
    }

    @Override
    public QuestionVO getQuestionById(Long qId) {
        QuestionBank q = questionBankMapper.selectById(qId);
        if (q == null) {
            throw new BaseException("题目不存在");
        }
        return QuestionVO.fromEntity(q);
    }

    @Override
    public PageResult listQuestions(Long courseId, String qType, Integer difficulty, String keyword,
                                    Integer page, Integer pageSize) {
        if (courseId == null) {
            throw new BaseException("课程ID不能为空");
        }
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1) ? 10 : pageSize;

        long total = questionBankMapper.countByConditions(courseId, qType, difficulty, keyword);
        List<QuestionBank> list = questionBankMapper.selectByConditionsWithLimit(
                courseId, qType, difficulty, keyword, (p - 1) * ps, ps);
        List<QuestionVO> records = list.stream().map(QuestionVO::fromEntity).toList();
        return new PageResult(total, records);
    }

    @Override
    public int countByCourseId(Long courseId) {
        return questionBankMapper.countByCourseId(courseId);
    }

    // ========== 内部方法 ==========

    private QuestionBank buildEntity(QuestionCreateDTO dto) {
        QuestionBank q = new QuestionBank();
        q.setCourseId(dto.getCourseId());
        q.setQType(dto.getQType());
        q.setContent(dto.getContent());
        q.setOptions(QuestionValidator.toJsonArray(dto.getOptions()));
        q.setAnswer(dto.getAnswer());
        q.setAnalysis(dto.getAnalysis());
        q.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : 2);
        q.setTags(QuestionValidator.toJsonArray(dto.getTags()));
        return q;
    }
}
