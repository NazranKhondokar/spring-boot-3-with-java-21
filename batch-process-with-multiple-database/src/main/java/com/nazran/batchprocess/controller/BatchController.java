package com.nazran.batchprocess.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batch")
public class BatchController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importEmployeeJob;

    @GetMapping("/start")
    public ResponseEntity<String> startBatchJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startAt", System.currentTimeMillis()) // Unique parameter to ensure the job runs again
                    .toJobParameters();

            jobLauncher.run(importEmployeeJob, jobParameters);
            return ResponseEntity.ok("Batch job has been invoked successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to trigger the batch job: " + e.getMessage());
        }
    }
}

