package com.nazran.chat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configures Firebase Admin SDK.
 */
@Slf4j
@Configuration
@EnableRetry
@EnableAsync
@EnableScheduling
public class FirebaseConfig {

    @Value("${firebase.config.path}")
    private String firebaseConfigPath;

    @Value("${BUCKET_NAME}")
    private String bucketName;

    private GoogleCredentials googleCredentials;
    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initializeFirebase() {
        try (InputStream serviceAccount = new FileInputStream(firebaseConfigPath)) {
            this.googleCredentials = GoogleCredentials.fromStream(serviceAccount);

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(googleCredentials)
                        .setStorageBucket(bucketName)
                        .build();

                this.firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase Initialized successfully.");
            } else {
                this.firebaseApp = FirebaseApp.getInstance();
                log.info("Firebase already initialized, using existing instance.");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize Firebase", e);
        }
    }

    @Bean
    public GoogleCredentials googleCredentials() {
        if (googleCredentials == null) {
            throw new IllegalStateException("GoogleCredentials has not been initialized yet.");
        }
        return googleCredentials;
    }
    // Add this new bean method
    @Bean
    public FirebaseAuth firebaseAuth() {
        return FirebaseAuth.getInstance(firebaseApp());
    }
    @Bean
    public FirebaseApp firebaseApp() {
        if (firebaseApp == null) {
            throw new IllegalStateException("FirebaseApp has not been initialized yet.");
        }
        return firebaseApp;
    }

    @Bean
    public Storage firebaseStorage() {
        return StorageOptions.newBuilder()
                .setCredentials(googleCredentials())
                .build()
                .getService();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        try {
            return FirebaseMessaging.getInstance(firebaseApp());
        } catch (Exception e) {
            log.error("Error creating FirebaseMessaging instance: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create FirebaseMessaging bean", e);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}