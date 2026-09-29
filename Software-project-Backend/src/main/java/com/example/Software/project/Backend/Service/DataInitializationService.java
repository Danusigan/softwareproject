package com.example.Software.project.Backend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(1) // Ensure this runs early during startup
public class DataInitializationService implements CommandLineRunner {

    @Autowired
    private ProgramOutcomeService programOutcomeService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== OBQA System Startup - Data Initialization ===");
        System.out.println("Program Outcomes auto-initialization is disabled.");
        System.out.println("Use admin action 'Initialize Defaults' to create Washington Accord PO1-PO12.");
        System.out.println("=== OBQA System Ready ===");
    }
}