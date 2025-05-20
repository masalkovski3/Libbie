package com.openlibrary.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mappa URL /profileImages/** till mappen profileImages/ i projektroten
        registry
                .addResourceHandler("/profileImages/**")
                .addResourceLocations("file:profileImages/");
    }
}
