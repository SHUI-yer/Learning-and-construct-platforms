package com.learning.lpstarter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 扫描整个项目所有包 com.learning 下所有子模块bean
@SpringBootApplication(scanBasePackages = "com.learning")
// 单独指定mapper接口路径
@MapperScan({"com.learning.lpauth.mapper","com.learning.lpcourse.mapper","com.learning.assessment.mapper"})
public class LpStarterApplication {
    public static void main(String[] args) {
        SpringApplication.run(LpStarterApplication.class, args);
    }
}