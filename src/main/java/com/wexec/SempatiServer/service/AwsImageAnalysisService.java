package com.wexec.SempatiServer.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

@Service
@Profile("prod") // Sadece PROD (AWS) ortamında çalışır
@RequiredArgsConstructor
@Slf4j
public class AwsImageAnalysisService implements IImageAnalysisService {

    private final AmazonRekognition rekognitionClient;
    private final AmazonS3 s3Client; // Videoyu geçici yüklemek için S3 client lazım

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final List<String> ALLOWED_LABELS = List.of("Cat", "Dog", "Pet", "Animal", "Puppy", "Kitten");

    @Override
    public void validateImageContent(MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType != null && contentType.startsWith("video")) {
            validateVideoContent(file);
        } else {
            validatePictureContent(file);
        }
    }

    // --- RESİM ANALİZİ (Eski Hızlı Yöntem) ---
    private void validatePictureContent(MultipartFile file) {
        log.info("--- GÖRÜNTÜ ANALİZİ: AWS REKOGNITION (RESİM) ---");
        try {
            ByteBuffer imageBytes = ByteBuffer.wrap(file.getBytes());
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image().withBytes(imageBytes))
                    .withMaxLabels(15)
                    .withMinConfidence(75F);

            DetectLabelsResult result = rekognitionClient.detectLabels(request);

            boolean isValid = result.getLabels().stream()
                    .anyMatch(label -> ALLOWED_LABELS.contains(label.getName()));

            if (!isValid) {
                log.warn("AWS: Resimde kedi/köpek bulunamadı.");
                throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        } catch (Exception e) {
            // Sadece beklenmeyen teknik hatalar buraya düşsün
            log.error("AWS Resim Hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AWS servisine ulaşılamıyor.");
        }
    }

    // --- VİDEO ANALİZİ (Yükle -> Analiz Et -> Sil) ---
    private void validateVideoContent(MultipartFile file) {
        log.info("--- GÖRÜNTÜ ANALİZİ: AWS REKOGNITION (VIDEO) ---");
        String tempFileName = "temp_analysis_" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            // 1. Videoyu S3'e GEÇİCİ olarak yükle
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            
            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(new PutObjectRequest(bucketName, tempFileName, inputStream, metadata));
            }

            // 2. Analizi Başlat
            StartLabelDetectionRequest startRequest = new StartLabelDetectionRequest()
                    .withVideo(new Video().withS3Object(new S3Object().withBucket(bucketName).withName(tempFileName)))
                    .withMinConfidence(70F);

            StartLabelDetectionResult startResult = rekognitionClient.startLabelDetection(startRequest);
            String jobId = startResult.getJobId();
            
            log.info("AWS Video Analizi Başladı. JobId: {}", jobId);

            // 3. Sonuç için Bekle (Polling)
            String status = "IN_PROGRESS";
            GetLabelDetectionResult result = null;

            // Maksimum 30 saniye bekle
            int maxTries = 15; 
            for (int i = 0; i < maxTries; i++) {
                Thread.sleep(2000); // 2 saniyede bir kontrol et
                
                GetLabelDetectionRequest getRequest = new GetLabelDetectionRequest().withJobId(jobId);
                result = rekognitionClient.getLabelDetection(getRequest);
                status = result.getJobStatus();
                
                if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
                    break;
                }
                log.info("AWS Analiz devam ediyor... ({}/{})", i+1, maxTries);
            }

            // 4. Sonucu Kontrol Et
            if (!"SUCCEEDED".equals(status)) {
                log.error("AWS Video Analizi Tamamlanamadı veya Zaman Aşımı. Status: {}", status);
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "Video analizi tamamlanamadı.");
            }

            boolean isValid = result.getLabels().stream()
                    .anyMatch(labelDetection -> ALLOWED_LABELS.contains(labelDetection.getLabel().getName()));

            if (!isValid) {
                log.warn("AWS: Videoda kedi/köpek bulunamadı.");
                throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AWS Video Hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AWS video servisine ulaşılamıyor.");
        } finally {
            // 5. TEMİZLİK: Geçici dosyayı S3'ten sil (Çok Önemli!)
            try {
                s3Client.deleteObject(bucketName, tempFileName);
                log.info("Geçici analiz dosyası silindi: {}", tempFileName);
            } catch (Exception e) {
                log.error("Geçici dosya silinemedi: {}", e.getMessage());
            }
        }
    }
}