package com.wexec.SempatiServer.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.wexec.SempatiServer.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageAnalysisService {

    private final AmazonRekognition rekognitionClient;

    // Geçerli saydığımız etiketler
    private static final List<String> ALLOWED_LABELS = List.of("Cat", "Dog", "Pet", "Animal", "Puppy", "Kitten");

    // PARAMETRE DEĞİŞTİ: Artık dosya ismi değil, dosyanın kendisini alıyor.
    public void validateImageContent(MultipartFile file) {

        try {
            // Dosyayı Byte (Sayısal veri) formatına çeviriyoruz
            ByteBuffer imageBytes = ByteBuffer.wrap(file.getBytes());

            // 1. AWS'ye sor: Bu veride ne var? (S3'e gitme, elimdekine bak)
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image().withBytes(imageBytes)) // <-- KRİTİK DEĞİŞİKLİK
                    .withMaxLabels(10)
                    .withMinConfidence(75F);

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> labels = result.getLabels();

            // 2. Gelen etiketleri kontrol et
            boolean isValid = false;
            for (Label label : labels) {
                // Loglara bas ki ne bulduğunu görelim (Konsolda)
                System.out.println("AI Analizi: " + label.getName() + " - %" + label.getConfidence());

                if (ALLOWED_LABELS.contains(label.getName())) {
                    isValid = true;
                    break;
                }
            }

            // 3. Kedi veya Köpek yoksa HATA FIRLAT
            if (!isValid) {
                throw new BusinessException("Yüklenen fotoğrafta kedi veya köpek tespit edilemedi.", "IMG_INVALID_CONTENT");
            }

        } catch (IOException e) {
            throw new BusinessException("Dosya okunamadı.", "FILE_READ_ERR");
        } catch (AmazonRekognitionException e) {
            throw new BusinessException("Yapay zeka servisi hatası: " + e.getMessage(), "AI_SERVICE_ERR");
        }
    }
}