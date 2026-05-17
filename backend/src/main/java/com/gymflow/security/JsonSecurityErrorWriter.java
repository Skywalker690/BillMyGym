package com.gymflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymflow.api.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonSecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, int status, String message, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(message, errorCode));
    }
}
