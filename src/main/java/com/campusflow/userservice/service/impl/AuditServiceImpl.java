package com.campusflow.userservice.service.impl;

import com.campusflow.userservice.service.AuditService;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final String COLLECTION_NAME = "audit_events";

    @Value("${gcp.project-id:GCP_PROJECT_ID_HERE}")
    private String projectId;

    private Firestore firestore;

    @PostConstruct
    public void init() {
        try {
            if (projectId != null && !projectId.equals("GCP_PROJECT_ID_HERE")) {
                firestore = FirestoreOptions.newBuilder()
                        .setProjectId(projectId)
                        .build()
                        .getService();
                log.info("Firestore initialized with project: {}", projectId);
            } else {
                log.warn("GCP Project ID not configured — Firestore audit logging disabled. " +
                         "Set GCP_PROJECT_ID environment variable to enable.");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize Firestore: {}. Audit logging will be disabled.", e.getMessage());
        }
    }

    @Override
    public void logEvent(String eventType, String userId, String requestId, String description, Map<String, Object> metadata) {
        if (firestore == null) {
            log.debug("Firestore not available — skipping audit event: {} for user: {}", eventType, userId);
            return;
        }

        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("userId", userId);
            event.put("description", description);
            event.put("timestamp", Instant.now().toString());

            if (requestId != null) {
                event.put("requestId", requestId);
            }
            if (metadata != null) {
                event.put("metadata", metadata);
            }

            firestore.collection(COLLECTION_NAME).add(event);
            log.info("Audit event logged: {} for user: {}", eventType, userId);
        } catch (Exception e) {
            // Audit logging should never break the main flow
            log.error("Failed to log audit event: {} — {}", eventType, e.getMessage());
        }
    }
}
