package com.wildai.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void internalBusinessExceptionReturnsHttp500AndCode500() throws Exception {
        mockMvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.message").value("图片保存失败"));
    }

    @Test
    void maxUploadSizeExceededReturnsHttp413AndCode413() throws Exception {
        mockMvc.perform(get("/test/upload-too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("413"))
                .andExpect(jsonPath("$.message").value("上传文件过大"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/internal-error")
        void internalError() {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }

        @GetMapping("/test/upload-too-large")
        void uploadTooLarge() {
            throw new MaxUploadSizeExceededException(52L * 1024 * 1024);
        }
    }
}
