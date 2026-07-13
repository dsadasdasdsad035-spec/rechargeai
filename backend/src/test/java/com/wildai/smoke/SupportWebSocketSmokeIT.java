package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 在线客服 WebSocket 冒烟测试：真实握手后双向实时推送。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("smoke")
@Testcontainers
class SupportWebSocketSmokeIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static org.testcontainers.containers.MySQLContainer<?> mysql =
            new org.testcontainers.containers.MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                    .withDatabaseName("wildai");

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void userAndAdminReceiveRealtimeSupportMessages() throws Exception {
        String userToken = registerUser();
        String adminToken = adminLogin();
        String sessionNo = createSession(userToken);

        QueueHandler userHandler = new QueueHandler();
        QueueHandler adminHandler = new QueueHandler();
        WebSocketSession userSocket = connect(userToken, userHandler);
        WebSocketSession adminSocket = connect(adminToken, adminHandler);

        try {
            userSocket.sendMessage(new TextMessage("""
                    {"type":"SEND_MESSAGE","sessionNo":"%s","content":"WebSocket用户消息"}
                    """.formatted(sessionNo)));
            assertThat(awaitEvent(adminHandler, "MESSAGE", "WebSocket用户消息")).isNotNull();

            adminSocket.sendMessage(new TextMessage("""
                    {"type":"SEND_MESSAGE","sessionNo":"%s","content":"WebSocket客服回复"}
                    """.formatted(sessionNo)));
            assertThat(awaitEvent(userHandler, "MESSAGE", "WebSocket客服回复")).isNotNull();
        } finally {
            userSocket.close();
            adminSocket.close();
        }
    }

    private WebSocketSession connect(String token, QueueHandler handler) throws Exception {
        String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return new StandardWebSocketClient()
                .execute(handler, "ws://localhost:" + port + "/ws/support?token=" + encoded)
                .get(5, TimeUnit.SECONDS);
    }

    private JsonNode awaitEvent(QueueHandler handler, String type, String content) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            String payload = handler.messages.poll(250, TimeUnit.MILLISECONDS);
            if (payload == null) {
                continue;
            }
            JsonNode event = objectMapper.readTree(payload);
            if (type.equals(event.path("type").asText())
                    && content.equals(event.path("message").path("content").asText())) {
                return event;
            }
        }
        throw new AssertionError("未收到 WebSocket 消息：" + content);
    }

    private String registerUser() throws Exception {
        String email = "support-ws" + System.currentTimeMillis() + "@example.com";
        MvcResult send = mockMvc.perform(post("/api/auth/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.devCode").isNotEmpty())
                .andReturn();
        String code = objectMapper.readTree(send.getResponse().getContentAsString())
                .get("data").get("devCode").asText();
        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EMAIL","email":"%s","verifyCode":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(register.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String adminLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"changeme\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String createSession(String userToken) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/support/sessions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"WebSocket咨询\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString())
                .get("data").get("sessionNo").asText();
    }

    static class QueueHandler extends TextWebSocketHandler {
        final LinkedBlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }
}
