package com.fooddelivery.upload;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadWebConfig implements WebMvcConfigurer {
    private final String dir;

    public UploadWebConfig(@Value("${app.upload.dir:uploads}") String dir) {
        this.dir = dir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(dir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(location)
            .setCachePeriod(3600);
    }
}
