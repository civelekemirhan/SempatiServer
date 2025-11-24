package com.wexec.SempatiServer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;      // Asıl veriler (Postlar)
    private int page;             // Şu anki sayfa no (0'dan başlar)
    private int size;             // Sayfada kaç veri var
    private long totalElements;   // Toplam kaç post var?
    private int totalPages;       // Toplam kaç sayfa var?
    private boolean last;         // Son sayfa mı? (Android buna göre "Daha fazla yükle"yi gizler)
}