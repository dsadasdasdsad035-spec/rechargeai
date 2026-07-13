package com.wildai.support.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.security.JwtTokenProvider;
import com.wildai.support.dto.SupportCreateSessionRequest;
import com.wildai.support.dto.SupportSendMessageRequest;
import com.wildai.support.dto.SupportWebSocketEvent;
import com.wildai.support.dto.SupportWebSocketInboundMessage;
import com.wildai.support.event.SupportMessageCreatedEvent;
import com.wildai.support.event.SupportSessionCreatedEvent;
import com.wildai.support.service.SupportService;
import io.jsonwebtoken.Claims;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SupportWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_AUDIENCE = "audience";
    private static final String ATTR_ID = "principalId";

    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final SupportService supportService;
    private final SupportOnlineStatus onlineStatus;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public SupportWebSocketHandler(ObjectMapper objectMapper,
                                   JwtTokenProvider jwtTokenProvider,
                                   SupportService supportService,
                                   SupportOnlineStatus onlineStatus) {
        this.objectMapper = objectMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.supportService = supportService;
        this.onlineStatus = onlineStatus;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = tokenFrom(session.getUri());
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("缺少登录凭证"));
            return;
        }
        try {
            Claims claims = jwtTokenProvider.parse(token);
            String audience = claims.getAudience().stream().findFirst().orElse("");
            Long principalId = Long.parseLong(claims.getSubject());
            session.getAttributes().put(ATTR_AUDIENCE, audience);
            session.getAttributes().put(ATTR_ID, principalId);
            if (JwtTokenProvider.AUD_ADMIN.equals(audience)) {
                onlineStatus.addAdminSession(session);
            } else if (JwtTokenProvider.AUD_USER.equals(audience)) {
                userSessions.computeIfAbsent(principalId, key -> ConcurrentHashMap.newKeySet()).add(session);
            } else {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("登录凭证无效"));
            }
        } catch (Exception ex) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("登录凭证无效"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SupportWebSocketInboundMessage inbound = objectMapper.readValue(message.getPayload(), SupportWebSocketInboundMessage.class);
        String audience = (String) session.getAttributes().get(ATTR_AUDIENCE);
        Long principalId = (Long) session.getAttributes().get(ATTR_ID);
        if (principalId == null || audience == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("登录凭证无效"));
            return;
        }

        if ("START_SESSION".equals(inbound.type()) && JwtTokenProvider.AUD_USER.equals(audience)) {
            var created = supportService.createSession(principalId, new SupportCreateSessionRequest(inbound.subject()));
            send(session, objectMapper.writeValueAsString(new SupportWebSocketEvent("SESSION_CREATED", created, null)));
            return;
        }

        if ("SEND_MESSAGE".equals(inbound.type()) && JwtTokenProvider.AUD_USER.equals(audience)) {
            supportService.sendUserMessage(principalId, inbound.sessionNo(), new SupportSendMessageRequest(inbound.content()));
            return;
        }

        if ("SEND_MESSAGE".equals(inbound.type()) && JwtTokenProvider.AUD_ADMIN.equals(audience)) {
            supportService.sendAdminMessage(principalId, inbound.sessionNo(), new SupportSendMessageRequest(inbound.content()));
            return;
        }

        send(session, objectMapper.writeValueAsString(Map.of("type", "ERROR", "message", "不支持的客服消息类型")));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        onlineStatus.removeAdminSession(session);
        Object principalId = session.getAttributes().get(ATTR_ID);
        if (principalId instanceof Long id) {
            Set<WebSocketSession> sessions = userSessions.get(id);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(id);
                }
            }
        }
    }

    @EventListener
    public void onSessionCreated(SupportSessionCreatedEvent event) {
        broadcastToAdmins(new SupportWebSocketEvent("SESSION_CREATED", event.session(), null));
    }

    @EventListener
    public void onMessageCreated(SupportMessageCreatedEvent event) {
        SupportWebSocketEvent payload = new SupportWebSocketEvent("MESSAGE", event.session(), event.message());
        broadcastToAdmins(payload);
        broadcastToUser(event.session().userId(), payload);
    }

    private void broadcastToAdmins(SupportWebSocketEvent payload) {
        sendAll(onlineStatus.adminSessions(), payload);
    }

    private void broadcastToUser(Long userId, SupportWebSocketEvent payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sendAll(sessions, payload);
        }
    }

    private void sendAll(Set<WebSocketSession> sessions, SupportWebSocketEvent payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            for (WebSocketSession session : sessions) {
                send(session, json);
            }
        } catch (IOException ignored) {
            // 单次广播失败不影响消息持久化。
        }
    }

    private void send(WebSocketSession session, String json) throws IOException {
        if (session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private String tokenFrom(URI uri) {
        if (uri == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
    }
}
