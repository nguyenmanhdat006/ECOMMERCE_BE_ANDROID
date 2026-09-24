package com.nguyendat.shopee_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
    
    @Value("${aws.s3.access-key:}")
    private String accessKey;
    
    @Value("${aws.s3.secret-key:}")
    private String secretKey;
    
    @Value("${aws.s3.region:us-east-1}")
    private String region;
    
    @Bean
    @ConditionalOnProperty(name = "file.storage.type", havingValue = "s3")
    public S3Client s3Client() {
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            throw new IllegalStateException("AWS S3 credentials not configured. Please set AWS_S3_ACCESS_KEY and AWS_S3_SECRET_KEY environment variables.");
        }
        
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
        
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
