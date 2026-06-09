package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.entity.AdminAuditLog;
import com.example.mapper.AdminAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AdminAuditLogMapper auditLogMapper;

    @Async
    public void log(Long operatorId, String operatorName, String action,
                    String targetType, Long targetId, String detail, String ip) {
        try {
            AdminAuditLog auditLog = new AdminAuditLog();
            auditLog.setOperatorId(operatorId);
            auditLog.setOperatorName(operatorName);
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setDetail(detail);
            auditLog.setIp(ip);
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志写入失败 action={} operatorId={}", action, operatorId, e);
        }
    }

    public Page<AdminAuditLog> queryPage(int page, int size, String action, String operatorName) {
        Page<AdminAuditLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AdminAuditLog> wrapper = new LambdaQueryWrapper<AdminAuditLog>()
                .eq(action != null && !action.isBlank(), AdminAuditLog::getAction, action)
                .like(operatorName != null && !operatorName.isBlank(), AdminAuditLog::getOperatorName, operatorName)
                .orderByDesc(AdminAuditLog::getCreateTime);
        return auditLogMapper.selectPage(pageParam, wrapper);
    }
}