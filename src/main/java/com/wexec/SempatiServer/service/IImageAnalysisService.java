package com.wexec.SempatiServer.service;

import org.springframework.web.multipart.MultipartFile;

public interface IImageAnalysisService {

    // Bu metot, resim geçerliyse sessizce devam eder.
    // Geçersizse (kedi/köpek yoksa) BusinessException fırlatır.
    void validateImageContent(MultipartFile file);
}