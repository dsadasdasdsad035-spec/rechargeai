package com.wildai.support.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SupportOnlineStatus {

    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();

    public void addAdminSession(WebSocketSession session) {
        adminSessions.add(session);
    }

    public void removeAdminSession(WebSocketSession session) {
        adminSessions.remove(session);
    }

    public boolean hasOnlineAdmin() {
        adminSessions.removeIf(session -> !session.isOpen());
        return !adminSessions.isEmpty();
    }

    Set<WebSocketSession> adminSessions() {
        return adminSessions;
    }
}
