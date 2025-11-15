package com.teamfiv5.fiv5.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account-path}")
    private String serviceAccountPath;

    @PostConstruct
    public void initializeFirebase() {
        try {
            Resource resource = new FileSystemResource(serviceAccountPath);
            if (!resource.exists()) {
                resource = new ClassPathResource(serviceAccountPath);
                if (!resource.exists()) {
                    log.error("Firebase 서비스 계정 파일을 찾을 수 없습니다: {}", serviceAccountPath);
                    return;
                }
            }

            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK가 성공적으로 초기화되었습니다.");
            }
        } catch (Exception e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
        }
    }
}