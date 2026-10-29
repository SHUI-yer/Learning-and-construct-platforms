package com.learning.learning.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.learning.learning.mapper")
public class MyBatisConfig {
}
