package com.gymflow.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "auth.google")
public class GoogleOAuthProperties {

    private List<String> audiences = new ArrayList<>();

    public List<String> sanitizedAudiences() {
        return audiences.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .toList();
    }
}
