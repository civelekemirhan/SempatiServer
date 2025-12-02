package com.wexec.SempatiServer.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode; // Import Eklendi
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

    private static final List<String> ALLOWED_LABELS = List.of("Cat", "Dog", "Pet", "Animal", "Puppy", "Kitten");

    public void validateImageContent(MultipartFile file) {

        try {
            ByteBuffer imageBytes = ByteBuffer.wrap(file.getBytes());

            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image().withBytes(imageBytes))
                    .withMaxLabels(10)
                    .withMinConfidence(75F);

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> labels = result.getLabels();

            boolean isValid = false;
            for (Label label : labels) {
                System.out.println("AI Analizi: " + label.getName() + " - %" + label.getConfidence());

                if (ALLOWED_LABELS.contains(label.getName())) {
                    isValid = true;
                    break;
                }
            }

            if (!isValid) {
                // String yerine Enum kullanıyoruz
                throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT);
            }

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        } catch (AmazonRekognitionException e) {
            // Detayı logda görmek için custom mesajlı constructor kullanabilirsin
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AWS Hatası: " + e.getMessage());
        }
    }
}