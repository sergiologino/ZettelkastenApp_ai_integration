package com.example.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class NoteappAiIntegrationApplication {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("🚀 Starting AI Integration Service");
        System.out.println("⚠️  Security AutoConfiguration DISABLED");
        System.out.println("✅ Using custom SecurityConfig");
        System.out.println("========================================");
        
        SpringApplication.run(NoteappAiIntegrationApplication.class, args);
    }

}
