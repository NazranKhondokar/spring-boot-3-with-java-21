package com.nazran.uploadlargefile.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressService {
    private final ConcurrentHashMap<String, Double> progressMap = new ConcurrentHashMap<>();

    public void updateProgress(String fileName, double progress) {
        progressMap.put(fileName, progress);
    }

    public Double getProgress(String fileName) {
        return progressMap.getOrDefault(fileName, 0.0);
    }

    public void clearProgress(String fileName) {
        progressMap.remove(fileName);
    }
}

