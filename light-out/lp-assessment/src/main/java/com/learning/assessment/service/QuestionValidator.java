package com.learning.assessment.service;

import com.learning.assessment.dto.QuestionCreateDTO;
import com.learning.lpcommon.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class QuestionValidator {

    public static final Set<String> Q_TYPES = Set.of(
            "single_choice", "multiple_choice", "judge", "fill_blank", "short_answer");

    private static final Map<String, String> TYPE_ALIASES = Map.ofEntries(
            Map.entry("单选", "single_choice"),
            Map.entry("single", "single_choice"),
            Map.entry("singlechoice", "single_choice"),
            Map.entry("多选", "multiple_choice"),
            Map.entry("multiple", "multiple_choice"),
            Map.entry("multiplechoice", "multiple_choice"),
            Map.entry("判断", "judge"),
            Map.entry("judge", "judge"),
            Map.entry("填空", "fill_blank"),
            Map.entry("fillblank", "fill_blank"),
            Map.entry("简答", "short_answer"),
            Map.entry("shortanswer", "short_answer"));

    public void validate(QuestionCreateDTO dto) {
        if (dto.getCourseId() == null) {
            throw new BaseException("课程ID不能为空");
        }
        if (dto.getQType() == null || !Q_TYPES.contains(dto.getQType())) {
            throw new BaseException("题型不合法，仅支持: single_choice/multiple_choice/judge/fill_blank/short_answer");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BaseException("题干内容不能为空");
        }
        if (dto.getAnswer() == null || dto.getAnswer().isBlank()) {
            throw new BaseException("参考答案不能为空");
        }

        switch (dto.getQType()) {
            case "single_choice" -> validateChoice(dto, false);
            case "multiple_choice" -> validateChoice(dto, true);
            case "judge" -> {
                dto.setOptions(null);
                if (!"0".equals(dto.getAnswer()) && !"1".equals(dto.getAnswer())) {
                    throw new BaseException("判断题答案仅允许 1(正确) 或 0(错误)");
                }
            }
            default -> dto.setOptions(null);
        }
    }

    /**
     * 将导入文件中的题型（中文/别名）规范化为题库题型编码，无法识别返回 null。
     */
    public String normalizeQType(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim().toLowerCase().replaceAll("\\s+", "");
        if (key.endsWith("题") && key.length() > 1) {
            key = key.substring(0, key.length() - 1);
        }
        return TYPE_ALIASES.get(key);
    }

    public static String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            if (inner.isBlank()) {
                return Collections.emptyList();
            }
            return Arrays.stream(inner.split(","))
                    .map(String::trim)
                    .map(s -> s.replaceAll("^\"|\"$", ""))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void validateChoice(QuestionCreateDTO dto, boolean multiple) {
        List<String> options = dto.getOptions();
        if (options == null || options.isEmpty()) {
            throw new BaseException("选择题必须设置选项");
        }
        if (options.size() < 2) {
            throw new BaseException("选择题至少需要2个选项");
        }
        for (String opt : options) {
            if (opt == null || opt.isBlank()) {
                throw new BaseException("选项内容不能为空");
            }
        }

        String answer = dto.getAnswer().trim();
        if (multiple) {
            List<String> indexStrs = Arrays.stream(answer.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (indexStrs.isEmpty()) {
                throw new BaseException("多选题至少需要一个正确答案");
            }
            Set<Integer> distinct = new LinkedHashSet<>();
            for (String idxStr : indexStrs) {
                int idx = parseIndex(idxStr, options.size());
                distinct.add(idx);
            }
            dto.setAnswer(distinct.stream().map(String::valueOf).collect(Collectors.joining(",")));
        } else {
            if (answer.contains(",")) {
                throw new BaseException("单选题只能设置一个答案");
            }
            parseIndex(answer, options.size());
        }
    }

    private int parseIndex(String idxStr, int optionSize) {
        int idx;
        try {
            idx = Integer.parseInt(idxStr);
        } catch (NumberFormatException e) {
            throw new BaseException("选择题答案下标必须是数字");
        }
        if (idx < 0 || idx >= optionSize) {
            throw new BaseException("选择题答案下标超出选项范围");
        }
        return idx;
    }
}
