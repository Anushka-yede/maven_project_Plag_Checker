package com.pc.api;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot application entry point for Plagiarism Checker Pro.
 *
 * <p>Component scan covers all pc.* packages so that beans defined in
 * pc-batch, pc-ai, etc. are auto-detected without explicit imports.
 */
@SpringBootApplication
@EnableBatchProcessing
@EnableAsync
@ComponentScan(basePackages = {"com.pc"}, excludeFilters = @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE, value = com.pc.batch.job.WebCheckJob.class))
public class PlagiarismCheckerApp {

    public static void main(String[] args) {
        SpringApplication.run(PlagiarismCheckerApp.class, args);
    }
}
