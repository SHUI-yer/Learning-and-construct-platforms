package com.learning.lpcourse.dto;

import com.learning.lpcommon.validation.ValidationGroups;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsIncompleteCourseCreateRequest() {
        CourseCreateDTO dto = new CourseCreateDTO();
        dto.setCourseName(" ");
        dto.setIsPublic(3);

        assertTrue(validator.validate(dto).size() >= 3);
    }

    @Test
    void updateGroupRequiresChapterIdAndCommonFields() {
        ChapterDTO dto = new ChapterDTO();
        dto.setCourseId(1L);
        dto.setTitle("第一章");

        assertTrue(validator.validate(dto, ValidationGroups.Update.class).stream()
                .anyMatch(violation -> "chapterId".equals(violation.getPropertyPath().toString())));
    }
}
