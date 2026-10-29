package com.learning.learning.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class LearningMqConfig {

    @Bean
    public TopicExchange learningExchange() {
        return new TopicExchange("learning.topic");
    }

    @Bean
    public Queue playReportQueue() {
        return new Queue("learning.play.report", true, false, false);
    }

    @Bean
    public Queue reportGenerateQueue() {
        return new Queue("learning.report.generate", true, false, false);
    }

    @Bean
    public Binding playReportBinding(Queue playReportQueue, TopicExchange learningExchange) {
        return BindingBuilder.bind(playReportQueue).to(learningExchange).with("learning.play.report");
    }

    @Bean
    public Binding reportGenerateBinding(Queue reportGenerateQueue, TopicExchange learningExchange) {
        return BindingBuilder.bind(reportGenerateQueue).to(learningExchange).with("learning.report.generate");
    }
}
