package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.AdminApproval;
import com.example.mapper.AdminApprovalMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员审批流程服务。
 * 敏感操作需发起审批，经另一位管理员审批后方可执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminApprovalService {

    private final AdminApprovalMapper adminApprovalMapper;

    /** 需要审批的敏感操作类型 */
    public static final java.util.Set<String> SENSITIVE_ACTIONS = java.util.Set.of(
            "ban_user", "unban_user", "batch_update_user",
            "delete_food", "delete_exercise",
            "export_users", "send_notification"
    );

    /**
     * 判断操作是否需要审批。
     */
    public boolean requiresApproval(String actionType) {
        return SENSITIVE_ACTIONS.contains(actionType);
    }

    /**
     * 发起审批申请。
     * @return 审批记录ID，后续用于轮询审批结果
     */
    @Transactional
    public Long requestApproval(Long operatorId, String operatorName,
                                 String actionType, String targetDescription,
                                 String requestPayload) {
        AdminApproval approval = new AdminApproval();
        approval.setOperatorId(operatorId);
        approval.setOperatorName(operatorName);
        approval.setActionType(actionType);
        approval.setTargetDescription(targetDescription);
        approval.setRequestPayload(requestPayload);
        approval.setStatus("pending");
        approval.setRequestedAt(LocalDateTime.now());
        approval.setExecuted(0);

        adminApprovalMapper.insert(approval);
        log.info("发起审批申请: id={} operator={} action={} target={}",
                approval.getId(), operatorName, actionType, targetDescription);
        return approval.getId();
    }

    /**
     * 审批通过。
     */
    @Transactional
    public AdminApproval approve(Long approvalId, Long approverId, String approverName, String reason) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval == null || !"pending".equals(approval.getStatus())) {
            return null;
        }
        approval.setStatus("approved");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApproveReason(reason);
        approval.setApprovedAt(LocalDateTime.now());
        adminApprovalMapper.updateById(approval);
        log.info("审批通过: id={} approver={} action={}", approvalId, approverName, approval.getActionType());
        return approval;
    }

    /**
     * 审批拒绝。
     */
    @Transactional
    public AdminApproval reject(Long approvalId, Long approverId, String approverName, String reason) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval == null || !"pending".equals(approval.getStatus())) {
            return null;
        }
        approval.setStatus("rejected");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApproveReason(reason);
        approval.setApprovedAt(LocalDateTime.now());
        adminApprovalMapper.updateById(approval);
        log.info("审批拒绝: id={} approver={} action={}", approvalId, approverName, approval.getActionType());
        return approval;
    }

    /**
     * 标记审批已执行。
     */
    @Transactional
    public void markExecuted(Long approvalId) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval != null) {
            approval.setExecuted(1);
            adminApprovalMapper.updateById(approval);
        }
    }

    /**
     * 获取待审批列表。
     */
    public List<AdminApproval> getPendingApprovals() {
        LambdaQueryWrapper<AdminApproval> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminApproval::getStatus, "pending")
                .orderByDesc(AdminApproval::getRequestedAt);
        return adminApprovalMapper.selectList(wrapper);
    }

    /**
     * 检查审批是否已通过。
     */
    public boolean isApproved(Long approvalId) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        return approval != null && "approved".equals(approval.getStatus()) && approval.getExecuted() == 0;
    }

    /**
     * 校验审批：敏感操作必须携带有效的已审批ID。
     * @return true=允许执行, false=需要审批
     */
    public boolean checkApproval(String actionType, Long approvalId, Long operatorId) {
        if (!requiresApproval(actionType)) {
            return true; // 非敏感操作，放行
        }
        if (approvalId == null) {
            log.warn("敏感操作未携带审批ID: action={} operatorId={}", actionType, operatorId);
            return false;
        }
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval == null || !"approved".equals(approval.getStatus())) {
            log.warn("审批无效或未通过: approvalId={} status={}", approvalId, approval != null ? approval.getStatus() : "null");
            return false;
        }
        if (approval.getExecuted() == 1) {
            log.warn("审批已执行过: approvalId={}", approvalId);
            return false;
        }
        if (!actionType.equals(approval.getActionType())) {
            log.warn("审批类型不匹配: expected={} actual={}", actionType, approval.getActionType());
            return false;
        }
        return true;
    }
}