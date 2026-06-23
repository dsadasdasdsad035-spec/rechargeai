package com.wildai.admin.service;

import com.wildai.admin.domain.AdminOperationLog;
import com.wildai.admin.repository.AdminOperationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AdminOperationLogRepository logRepo;

    public AuditLogService(AdminOperationLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    @Transactional
    public void log(Long operatorId, String operationType, String targetType, String targetId,
                    String beforeValue, String afterValue, String reason) {
        AdminOperationLog entry = new AdminOperationLog();
        entry.setOperatorId(operatorId);
        entry.setOperationType(operationType);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setBeforeValue(beforeValue);
        entry.setAfterValue(afterValue);
        entry.setReason(reason);
        logRepo.save(entry);
    }
}
