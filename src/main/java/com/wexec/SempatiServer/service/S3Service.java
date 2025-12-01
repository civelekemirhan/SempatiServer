package com.wexec.SempatiServer.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Yüklenecek dosya boş olamaz.");
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));

            return s3Client.getUrl(bucketName, fileName).toString();

        } catch (IOException e) {
            // Dosya okuma hatası
            log.error("Dosya okuma hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);

        } catch (AmazonServiceException e) {
            // AWS S3 kaynaklı hata (Bucket yok, izin yok, AWS çökük vs.)
            log.error("AWS S3 Yükleme Hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "AWS Hatası: " + e.getErrorMessage());

        } catch (Exception e) {
            // Beklenmeyen diğer hatalar
            log.error("Beklenmeyen yükleme hatası: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }
}