package com.wildai.admin.service;

import com.wildai.admin.domain.AdminOperationLog;
import com.wildai.admin.dto.AuditLogDto;
import com.wildai.admin.repository.AdminOperationLogRepository;
import com.wildai.admin.repository.AdminUserRepository;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminAuditLogQueryService {

    private final AdminOperationLogRepository logRepo;
    private final AdminUserRepository adminUserRepo;
    private final RbacService rbacService;

    public AdminAuditLogQueryService(AdminOperationLogRepository logRepo,
                                     AdminUserRepository adminUserRepo,
                                     RbacService rbacService) {
        this.logRepo = logRepo;
        this.adminUserRepo = adminUserRepo;
        this.rbacService = rbacService;
    }

    public PageResult<AuditLogDto> search(Long viewerId, Long operatorId, String operationType,
                                          String targetType, String targetId,
                                          Instant start, Instant end,
                                          int pageNo, int pageSize) {
        if (!rbacService.canViewAuditLogs(viewerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前角色不可查看审计日志");
        }
        var page = logRepo.search(operatorId, operationType, targetType, targetId,
                start, end, PageRequest.of(pageNo - 1, pageSize));
        var items = page.getContent().stream().map(this::toDto).toList();
        return new PageResult<>(pageNo, pageSize, page.getTotalElements(), items);
    }

    private AuditLogDto toDto(AdminOperationLog log) {
        String operatorName = adminUserRepo.findById(log.getOperatorId())
                .map(a -> a.getDisplayName() != null ? a.getDisplayName() : a.getUsername())
                .orElse("未知");
        return new AuditLogDto(
                log.getId(),
                log.getOperatorId(),
                operatorName,
                log.getOperationType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getBeforeValue(),
                log.getAfterValue(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
