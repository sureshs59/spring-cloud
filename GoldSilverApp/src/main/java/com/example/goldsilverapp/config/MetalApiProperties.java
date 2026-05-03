package com.example.goldsilverapp.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "metal.api")
@NoArgsConstructor
@Setter
@Getter
public class MetalApiProperties {
    private String baseUrl;
    private String key;
}
