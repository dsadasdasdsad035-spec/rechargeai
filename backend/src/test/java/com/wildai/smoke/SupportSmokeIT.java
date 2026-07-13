package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 在线客服冒烟测试：用户发起会话，管理员回复，双方可以读取同一条消息线索。
 */
class SupportSmokeIT extends BaseSmokeIT {

    @Test
    void userAndAdminCanChatThroughSupportSession() throws Exception {
        String userToken = registerUser("support-user");
        String adminToken = adminLogin();

        MvcResult created = mockMvc.perform(post("/api/support/sessions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"支付咨询\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.sessionNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();

        String sessionNo = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("sessionNo").asText();

        mockMvc.perform(post("/api/support/sessions/" + sessionNo + "/messages")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"订单支付后没有到账\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.senderType").value("USER"))
                .andExpect(jsonPath("$.data.content").value("订单支付后没有到账"));

        mockMvc.perform(get("/admin/api/support/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items[0].sessionNo").value(sessionNo))
                .andExpect(jsonPath("$.data.items[0].subject").value("支付咨询"))
                .andExpect(jsonPath("$.data.items[0].lastMessage").value("订单支付后没有到账"))
                .andExpect(jsonPath("$.data.items[0].unreadAdminCount").value(1));

        mockMvc.perform(post("/admin/api/support/sessions/" + sessionNo + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"您好，请提供订单号，我来帮您核查。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.senderType").value("ADMIN"))
                .andExpect(jsonPath("$.data.content").value("您好，请提供订单号，我来帮您核查。"));

        mockMvc.perform(get("/api/support/sessions/" + sessionNo + "/messages")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items[0].senderType").value("USER"))
                .andExpect(jsonPath("$.data.items[0].content").value("订单支付后没有到账"))
                .andExpect(jsonPath("$.data.items[1].senderType").value("ADMIN"))
                .andExpect(jsonPath("$.data.items[1].content").value("您好，请提供订单号，我来帮您核查。"));

        mockMvc.perform(get("/admin/api/support/sessions/" + sessionNo + "/messages")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.items[0].senderType").value("USER"))
                .andExpect(jsonPath("$.data.items[1].senderType").value("ADMIN"));
    }

    private String registerUser(String prefix) throws Exception {
        String email = prefix + System.currentTimeMillis() + "@example.com";
        String code = sendEmailCode(email);
        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(register.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }
}
