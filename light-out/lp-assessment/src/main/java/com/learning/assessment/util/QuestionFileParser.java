package com.learning.assessment.util;

import com.learning.assessment.dto.QuestionImportRow;
import com.learning.lpcommon.exception.BaseException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量导入文件解析：Excel(.xlsx/.xls) 按固定列模板，Word(.docx) 按字段标签/题号规则。
 *
 * <p>Excel 模板列（第 1 行为表头）：题型 | 题干 | 选项A | 选项B | 选项C | 选项D | 答案 | 解析 | 难度 | 标签</p>
 * <p>Word 约定：题号/题干行开头，选项行形如 "A. xxx"，字段行形如 "题型：/答案：/解析：/难度：/标签：" 或 【题型】…。</p>
 */
@Component
public class QuestionFileParser {

    private static final DataFormatter FORMATTER = new DataFormatter();
    private static final Pattern OPTION_PATTERN = Pattern.compile("^[A-Da-d][.、)）]\\s*(.+)$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^(题型|答案|解析|难度|标签)[:：]\\s*(.*)$");
    private static final Pattern BRACKET_FIELD_PATTERN = Pattern.compile("^【(题型|答案|解析|难度|标签)】\\s*(.*)$");
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\d+[.、)）]");

    public List<QuestionImportRow> parse(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            throw new BaseException("导入文件内容为空");
        }
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(data, lower);
        }
        if (lower.endsWith(".docx")) {
            return parseWord(data);
        }
        throw new BaseException("仅支持 .xlsx/.xls/.docx 格式文件");
    }

    // ========== Excel ==========

    private List<QuestionImportRow> parseExcel(byte[] data, String filename) {
        List<QuestionImportRow> rows = new ArrayList<>();
        try (Workbook wb = filename.endsWith(".xlsx")
                ? new XSSFWorkbook(new ByteArrayInputStream(data))
                : new HSSFWorkbook(new ByteArrayInputStream(data))) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                QuestionImportRow item = new QuestionImportRow();
                item.setRowNo(i + 1);
                item.setQType(cellText(row.getCell(0)));
                item.setContent(cellText(row.getCell(1)));
                List<String> options = new ArrayList<>();
                for (int c = 2; c <= 5; c++) {
                    String opt = cellText(row.getCell(c));
                    if (!opt.isBlank()) {
                        options.add(opt);
                    }
                }
                item.setOptions(options);
                item.setAnswer(cellText(row.getCell(6)));
                item.setAnalysis(cellText(row.getCell(7)));
                parseDifficulty(item, cellText(row.getCell(8)));
                item.setTags(splitTags(cellText(row.getCell(9))));
                if (item.isEmpty()) {
                    continue;
                }
                rows.add(item);
            }
        } catch (IOException e) {
            throw new BaseException("文件解析失败，请确认文件未损坏且格式正确");
        }
        if (rows.isEmpty()) {
            throw new BaseException("文件中未读取到题目数据（第1行为表头，请从第2行开始填写）");
        }
        return rows;
    }

    // ========== Word ==========

    private List<QuestionImportRow> parseWord(byte[] data) {
        List<QuestionImportRow> rows = new ArrayList<>();
        QuestionImportRow current = null;
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String line = para.getText() == null ? "" : para.getText().trim();
                if (line.isEmpty()) {
                    continue;
                }
                Matcher field = FIELD_PATTERN.matcher(line);
                Matcher bracketField = BRACKET_FIELD_PATTERN.matcher(line);
                if (field.matches()) {
                    if (current == null) {
                        current = newRow(rows);
                    }
                    applyField(current, field.group(1), field.group(2));
                    continue;
                }
                if (bracketField.matches()) {
                    if (current == null) {
                        current = newRow(rows);
                    }
                    applyField(current, bracketField.group(1), bracketField.group(2));
                    continue;
                }
                Matcher opt = OPTION_PATTERN.matcher(line);
                if (opt.matches()) {
                    if (current == null) {
                        current = newRow(rows);
                    }
                    current.getOptions().add(opt.group(1).trim());
                    continue;
                }
                if (current == null) {
                    current = newRow(rows);
                } else if (!current.getContent().isBlank() && NUMBERED_PATTERN.matcher(line).find()) {
                    current = newRow(rows);
                }
                if (current.getContent().isBlank()) {
                    current.setContent(line);
                } else {
                    current.setContent(current.getContent() + "\n" + line);
                }
            }
        } catch (IOException e) {
            throw new BaseException("Word 文件解析失败，请确认文件未损坏且格式正确");
        }
        if (rows.isEmpty()) {
            throw new BaseException("Word 文件中未解析到题目，请按约定格式填写");
        }
        return rows;
    }

    private QuestionImportRow newRow(List<QuestionImportRow> rows) {
        QuestionImportRow row = new QuestionImportRow();
        row.setRowNo(rows.size() + 1);
        rows.add(row);
        return row;
    }

    private void applyField(QuestionImportRow row, String key, String value) {
        switch (key) {
            case "题型" -> row.setQType(value.trim());
            case "答案" -> row.setAnswer(value.trim());
            case "解析" -> row.setAnalysis(value.trim());
            case "难度" -> parseDifficulty(row, value.trim());
            case "标签" -> row.setTags(splitTags(value));
            default -> {
            }
        }
    }

    private void parseDifficulty(QuestionImportRow row, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            int d = Integer.parseInt(text.trim());
            if (d < 1 || d > 3) {
                row.setError("难度必须为1/2/3");
            } else {
                row.setDifficulty(d);
            }
        } catch (NumberFormatException e) {
            row.setError("难度必须为数字1/2/3");
        }
    }

    private String cellText(Cell cell) {
        return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
    }

    private List<String> splitTags(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<String> tags = Arrays.stream(text.split("[,，;；、]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return tags.isEmpty() ? null : tags;
    }
}
