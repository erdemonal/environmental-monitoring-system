package com.ecoguard.ecoguard.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;

/**
 * Initializes Firebase Admin SDK using the service account in resources.
 * Expects a file named "service-account-key.json" under classpath resources.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        try (InputStream serviceAccount = getClass().getClassLoader()
                .getResourceAsStream("service-account-key.json")) {

            if (serviceAccount == null) {
                logger.warn("Firebase service-account-key.json not found; push will be disabled.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized for push notifications.");
            }
        } catch (Exception e) {
            logger.error("Firebase initialization failed", e);
        }
    }
}

