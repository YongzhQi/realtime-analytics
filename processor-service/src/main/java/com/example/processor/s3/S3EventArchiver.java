package com.example.processor.s3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3EventArchiver {
    
    private static final Logger logger = LoggerFactory.getLogger(S3EventArchiver.class);
    private static final int BATCH_SIZE = 1000;
    
    @Value("${s3.bucket.name:realtime-analytics-yongqi}")
    private String bucketName;
    
    @Value("${s3.enabled:false}")
    private boolean s3Enabled;
    
    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final List<String> eventBatch;
    private long lastFlushTime;
    
    public S3EventArchiver() {
        // Get region from environment variable, default to US_EAST_2
        String awsRegion = System.getenv("AWS_DEFAULT_REGION");
        Region region = awsRegion != null ? Region.of(awsRegion) : Region.US_EAST_2;
        
        this.s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        this.objectMapper = new ObjectMapper();
        this.eventBatch = new ArrayList<>();
        this.lastFlushTime = System.currentTimeMillis();
    }
    
    public void archiveEvent(String eventJson) {
        if (!s3Enabled) {
            return;
        }
        
        synchronized (eventBatch) {
            eventBatch.add(eventJson);
            
            // Flush if batch is full or 60 seconds have passed
            if (eventBatch.size() >= BATCH_SIZE || 
                (System.currentTimeMillis() - lastFlushTime) > 60000) {
                flushToS3();
            }
        }
    }
    
    private void flushToS3() {
        if (eventBatch.isEmpty()) {
            return;
        }
        
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));
            String fileName = String.format("events/%s/events-%d.json", timestamp, System.currentTimeMillis());
            
            String batchJson = String.join("\n", eventBatch);
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("application/json")
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromString(batchJson));
            
            logger.info("Archived {} events to S3: s3://{}/{}", eventBatch.size(), bucketName, fileName);
            
            eventBatch.clear();
            lastFlushTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.error("Failed to archive events to S3", e);
        }
    }
}