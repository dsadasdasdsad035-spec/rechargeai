package com.wildai.support.service;

import com.wildai.auth.repository.UserAccountRepository;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.support.domain.SupportMessage;
import com.wildai.support.domain.SupportSession;
import com.wildai.support.dto.SupportCreateSessionRequest;
import com.wildai.support.dto.SupportMessageDto;
import com.wildai.support.dto.SupportSendMessageRequest;
import com.wildai.support.dto.SupportSessionDto;
import com.wildai.support.event.SupportMessageCreatedEvent;
import com.wildai.support.event.SupportSessionCreatedEvent;
import com.wildai.support.repository.SupportMessageRepository;
import com.wildai.support.repository.SupportSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SupportService {

    private final SupportSessionRepository sessionRepo;
    private final SupportMessageRepository messageRepo;
    private final UserAccountRepository userRepo;
    private final ApplicationEventPublisher eventPublisher;

    public SupportService(SupportSessionRepository sessionRepo,
                          SupportMessageRepository messageRepo,
                          UserAccountRepository userRepo,
                          ApplicationEventPublisher eventPublisher) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SupportSessionDto createSession(Long userId, SupportCreateSessionRequest req) {
        userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        SupportSession session = new SupportSession();
        session.setSessionNo("CS" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        session.setUserId(userId);
        session.setSubject(cleanSubject(req.subject()));
        session.setStatus("OPEN");
        SupportSession saved = sessionRepo.save(session);
        SupportSessionDto dto = toSessionDto(saved);
        eventPublisher.publishEvent(new SupportSessionCreatedEvent(dto));
        return dto;
    }

    public PageResult<SupportSessionDto> listUserSessions(Long userId, int pageNo, int pageSize) {
        var page = sessionRepo.findByUserIdOrderByUpdatedAtDesc(userId, PageRequest.of(pageNo - 1, pageSize));
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), page.getContent().stream()
                .map(this::toSessionDto)
                .toList());
    }

    public PageResult<SupportSessionDto> listAdminSessions(int pageNo, int pageSize) {
        var page = sessionRepo.findAllByOrderByUpdatedAtDesc(PageRequest.of(pageNo - 1, pageSize));
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), page.getContent().stream()
                .map(this::toSessionDto)
                .toList());
    }

    @Transactional
    public PageResult<SupportMessageDto> getUserMessages(Long userId, String sessionNo, int pageNo, int pageSize) {
        SupportSession session = requireUserSession(userId, sessionNo);
        session.setUnreadUserCount(0);
        session.setUpdatedAt(Instant.now());
        return listMessages(session, pageNo, pageSize);
    }

    @Transactional
    public PageResult<SupportMessageDto> getAdminMessages(String sessionNo, int pageNo, int pageSize) {
        SupportSession session = requireSession(sessionNo);
        session.setUnreadAdminCount(0);
        session.setUpdatedAt(Instant.now());
        return listMessages(session, pageNo, pageSize);
    }

    @Transactional
    public SupportMessageDto sendUserMessage(Long userId, String sessionNo, SupportSendMessageRequest req) {
        SupportSession session = requireUserSession(userId, sessionNo);
        SupportMessageDto message = appendMessage(session, "USER", userId, req.content(), true);
        eventPublisher.publishEvent(new SupportMessageCreatedEvent(toSessionDto(session), message));
        return message;
    }

    @Transactional
    public SupportMessageDto sendAdminMessage(Long adminId, String sessionNo, SupportSendMessageRequest req) {
        SupportSession session = requireSession(sessionNo);
        SupportMessageDto message = appendMessage(session, "ADMIN", adminId, req.content(), false);
        eventPublisher.publishEvent(new SupportMessageCreatedEvent(toSessionDto(session), message));
        return message;
    }

    private SupportMessageDto appendMessage(SupportSession session, String senderType, Long senderId, String content, boolean fromUser) {
        if (!"OPEN".equals(session.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "客服会话已关闭");
        }
        String trimmed = cleanContent(content);
        Instant now = Instant.now();
        SupportMessage message = new SupportMessage();
        message.setSessionId(session.getId());
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setContent(trimmed);
        message.setCreatedAt(now);
        SupportMessage saved = messageRepo.save(message);

        session.setLastMessage(trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed);
        session.setLastMessageAt(now);
        session.setUpdatedAt(now);
        if (fromUser) {
            session.setUnreadAdminCount(session.getUnreadAdminCount() + 1);
        } else {
            session.setUnreadUserCount(session.getUnreadUserCount() + 1);
        }
        return toMessageDto(session, saved);
    }

    private PageResult<SupportMessageDto> listMessages(SupportSession session, int pageNo, int pageSize) {
        var page = messageRepo.findBySessionIdOrderByCreatedAtAsc(session.getId(), PageRequest.of(pageNo - 1, pageSize));
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), page.getContent().stream()
                .map(message -> toMessageDto(session, message))
                .toList());
    }

    private SupportSession requireUserSession(Long userId, String sessionNo) {
        SupportSession session = requireSession(sessionNo);
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private SupportSession requireSession(String sessionNo) {
        return sessionRepo.findBySessionNo(sessionNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private SupportSessionDto toSessionDto(SupportSession session) {
        return new SupportSessionDto(
                session.getSessionNo(),
                session.getUserId(),
                session.getSubject(),
                session.getStatus(),
                session.getLastMessage(),
                session.getUnreadUserCount(),
                session.getUnreadAdminCount(),
                session.getLastMessageAt(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    private SupportMessageDto toMessageDto(SupportSession session, SupportMessage message) {
        return new SupportMessageDto(
                message.getId(),
                session.getSessionNo(),
                message.getSenderType(),
                message.getSenderId(),
                message.getContent(),
                message.getCreatedAt());
    }

    private String cleanSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入咨询主题");
        }
        return subject.trim();
    }

    private String cleanContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入消息内容");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 1000) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息内容不能超过1000字");
        }
        return trimmed;
    }
}
