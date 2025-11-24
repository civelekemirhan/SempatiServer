package com.wexec.SempatiServer.dto;

import com.wexec.SempatiServer.entity.PostType;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Data
public class PostRequest {
    private String description;
    private PostType type;

    // Konum (Kayıp ilanları için zorunlu olabilir frontend'de)
    private Double latitude;
    private Double longitude;
    private String address;

    private List<MultipartFile> images; // Çoklu resim yükleme
}