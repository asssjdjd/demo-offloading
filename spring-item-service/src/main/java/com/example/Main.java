package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main - Entry Point của Spring Boot Application cho Item Service
 * 
 * Microservice này chịu trách nhiệm quản lý sản phẩm (Items) trong hệ thống
 * 
 * Port: 8082
 * Context Path: /
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.example"})
public class Main {
    
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        System.out.println("\n====================================");
        System.out.println("✅ Item Service started successfully!");
        System.out.println("📍 URL: http://localhost:8082");
        System.out.println("📚 Swagger UI: http://localhost:8082/swagger-ui.html");
        System.out.println("====================================\n");
    }
}