package com.soumyajit.jharkhand_project.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // This enables @Scheduled annotation in CartService
}
