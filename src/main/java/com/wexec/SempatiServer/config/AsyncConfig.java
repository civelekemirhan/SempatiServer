package com.wexec.SempatiServer.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // BURAYI AYNI ANDA BİRDEN FAZLA CİHAZADAN MAİL ATILABİLMESİNİ SAĞLAMAK İÇİN EKLEDİM
    // DOĞRULUĞU KONTROL EDİLEBİLİR

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // Aynı anda çalışacak minimum thread
        executor.setMaxPoolSize(10);   // Yük artarsa çıkabileceği maksimum thread
        executor.setQueueCapacity(500); // Sıraya alabileceği işlem sayısı
        executor.setThreadNamePrefix("SempatiAsync-");
        executor.initialize();
        return executor;
    }
}