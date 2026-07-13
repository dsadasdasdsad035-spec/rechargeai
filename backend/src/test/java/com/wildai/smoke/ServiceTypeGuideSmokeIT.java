package com.wildai.smoke;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceTypeGuideSmokeIT extends BaseSmokeIT {

    @Test
    void serviceTypeGuideListIncludesNewAiServiceTypes() throws Exception {
        String adminToken = adminLogin();

        mockMvc.perform(get("/admin/api/service-types")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(content().string(containsString("X_PREMIUM_PLUS")))
                .andExpect(content().string(containsString("X Premium+")))
                .andExpect(content().string(containsString("GEMINI_PRO_ULTRA")))
                .andExpect(content().string(containsString("Gemini Pro / Ultra")))
                .andExpect(content().string(containsString("CLAUDE_API")))
                .andExpect(content().string(containsString("Claude API")));
    }
}
