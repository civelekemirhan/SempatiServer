package com.wexec.SempatiServer.service;

import com.wexec.SempatiServer.common.BusinessException;
import com.wexec.SempatiServer.common.ErrorCode;
import com.wexec.SempatiServer.dto.YoloResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;

@Service
@Profile("dev")
@Slf4j
public class LocalImageAnalysisService implements IImageAnalysisService {

    private final WebClient webClient;

    public LocalImageAnalysisService(WebClient.Builder webClientBuilder,
            @Value("${python.service.url:http://127.0.0.1:8000}") String pythonServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(pythonServiceUrl).build();
    }

    @Override
    public void validateImageContent(MultipartFile file) {
        log.info("--- GÖRÜNTÜ/VİDEO ANALİZİ: LOCAL YOLO (PYTHON) ---");
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            // YENİ KISIM: Dosya türünü dinamik belirliyoruz
            String contentType = file.getContentType();
            String typeToSend = (contentType != null && contentType.startsWith("video")) ? "video" : "image";

            body.add("file_type", typeToSend); // "image" veya "video" gidecek

            YoloResponse response = webClient.post()
                    .uri("/detect")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(YoloResponse.class)
                    .block();

            if (response == null) {
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
            }

            if (!response.isContains_animal()) {
                // Video için farklı log mesajı
                if ("video".equals(typeToSend)) {
                    log.warn("YOLO: Videoda kedi/köpek bulunamadı.");
                } else {
                    log.warn("YOLO: Resimde kedi/köpek bulunamadı.");
                }
                throw new BusinessException(ErrorCode.IMAGE_INVALID_CONTENT);
            }

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_READ_ERROR);
        } catch (Exception e) {
            log.error("Python servisine bağlanılamadı: {}", e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "Yapay zeka servisine ulaşılamıyor.");
        }
    }
}