package com.lanyu.xiaolanaitravel.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyu.xiaolanaitravel.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/** Writes the same JSON error contract from MVC interceptors and controllers. */
@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                status, message, request.getRequestURI(), LocalDateTime.now()));
    }
}
