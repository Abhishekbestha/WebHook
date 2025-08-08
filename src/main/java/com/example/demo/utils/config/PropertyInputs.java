package com.example.demo.utils.config;

import lombok.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;

@Configuration
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropertyInputs {
    @Value("${FOLDER_PATH}")
    private String FOLDER_PATH;

    @Getter
    private static PropertyInputs propertyInputs;

    @PostConstruct
    public void init() {
        propertyInputs = this;
    }
}
