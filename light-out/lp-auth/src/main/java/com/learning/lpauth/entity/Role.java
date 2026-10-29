package com.learning.lpauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Integer roleId;
    private String roleName;
    private String roleCode;
    private Integer level;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
