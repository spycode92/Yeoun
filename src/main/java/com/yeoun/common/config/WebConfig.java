package com.yeoun.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.uploadBaseLocation}")
    private String uploadBaseLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // /uploads/** → 실제 파일 폴더를 정적으로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBaseLocation + "/")
                .setCachePeriod(3600 * 24 * 30); // 옵션: 캐시 30일

        // 이미지 같은 정적 리소스를 안전하게 서버에서 직접 제공
    }
}
