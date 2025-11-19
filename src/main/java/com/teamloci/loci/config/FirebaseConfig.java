package com.teamloci.loci.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

    private FirebaseApp firebaseApp;

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
                this.firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK가 성공적으로 초기화되었습니다.");
            } else {
                this.firebaseApp = FirebaseApp.getInstance();
            }
        } catch (Exception e) {
            log.error("Firebase Admin SDK 초기화 실패", e);
        }
    }

    @Bean
    public Firestore firestore() {
        if (this.firebaseApp == null) {
            throw new IllegalStateException("FirebaseApp이 초기화되지 않았습니다.");
        }
        return FirestoreClient.getFirestore(this.firebaseApp);
    }
}