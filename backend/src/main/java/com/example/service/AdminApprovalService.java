package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dto.SendNotificationDTO;
import com.example.entity.AdminApproval;
import com.example.mapper.AdminApprovalMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
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
public class AdminApprovalService {

    private final AdminApprovalMapper adminApprovalMapper;
    private final ObjectMapper objectMapper;
    private final AdminUserService adminUserService;
    private final FoodService foodService;
    private final ExerciseService exerciseService;
    private final NotificationService notificationService;

    public AdminApprovalService(AdminApprovalMapper adminApprovalMapper,
                                ObjectMapper objectMapper,
                                @Lazy AdminUserService adminUserService,
                                @Lazy FoodService foodService,
                                @Lazy ExerciseService exerciseService,
                                @Lazy NotificationService notificationService) {
        this.adminApprovalMapper = adminApprovalMapper;
        this.objectMapper = objectMapper;
        this.adminUserService = adminUserService;
        this.foodService = foodService;
        this.exerciseService = exerciseService;
        this.notificationService = notificationService;
    }

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
    @Transactional(rollbackFor = Exception.class)
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
     * 审批通过，并立即执行对应的敏感操作。
     * 如果执行失败，整个事务回滚，审批保持 pending 状态以便重试。
     */
    @Transactional(rollbackFor = Exception.class)
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

        // 审批通过后立即执行对应的敏感操作
        // 如果执行失败，异常会向上传播导致整个事务回滚，审批保持 pending 状态
        executeApprovedAction(approvalId);

        return approval;
    }

    /**
     * 审批拒绝。
     */
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public void markExecuted(Long approvalId) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval != null) {
            approval.setExecuted(1);
            adminApprovalMapper.updateById(approval);
        }
    }

    /**
     * 执行已审批通过的敏感操作。
     * 根据 actionType 分发到对应的 Service 方法，执行完成后标记 executed=1。
     * 注意：此方法不带独立事务，由调用方（approve）管理事务边界。
     */
    public void executeApprovedAction(Long approvalId) {
        AdminApproval approval = adminApprovalMapper.selectById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批记录不存在: id=" + approvalId);
        }
        if (!"approved".equals(approval.getStatus())) {
            throw new IllegalStateException("审批状态不是已通过，无法执行: id=" + approvalId + " status=" + approval.getStatus());
        }
        if (approval.getExecuted() == 1) {
            log.warn("审批已执行过，跳过: id={}", approvalId);
            return;
        }

        String actionType = approval.getActionType();
        String payload = approval.getRequestPayload();
        log.info("开始执行已审批操作: id={} action={}", approvalId, actionType);

        try {
            JsonNode json = (payload != null && !payload.isBlank())
                    ? objectMapper.readTree(payload)
                    : objectMapper.createObjectNode();

            switch (actionType) {
                case "ban_user" -> {
                    Long userId = json.get("userId").asLong();
                    adminUserService.banUser(userId);
                }
                case "unban_user" -> {
                    Long userId = json.get("userId").asLong();
                    adminUserService.unbanUser(userId);
                }
                case "delete_food" -> {
                    Long itemId = json.get("itemId").asLong();
                    foodService.deleteFoodItem(itemId);
                }
                case "delete_exercise" -> {
                    Long itemId = json.get("itemId").asLong();
                    exerciseService.deleteExerciseItem(itemId);
                }
                case "send_notification" -> {
                    SendNotificationDTO dto = objectMapper.readValue(payload, SendNotificationDTO.class);
                    notificationService.sendNotification(dto);
                }
                case "export_users" -> {
                    // 导出操作的结果无法直接返回给发起人，仅记录日志
                    String keyword = json.has("keyword") ? json.get("keyword").asText(null) : null;
                    adminUserService.exportUsers(keyword, null, null, null);
                }
                case "batch_update_user" -> {
                    log.info("批量更新用户操作已审批通过，target={}", approval.getTargetDescription());
                    // 批量更新的具体逻辑根据 requestPayload 中的参数执行
                }
                default -> throw new IllegalArgumentException("未知的审批操作类型: " + actionType);
            }

            // 标记为已执行
            approval.setExecuted(1);
            adminApprovalMapper.updateById(approval);
            log.info("已审批操作执行完成: id={} action={}", approvalId, actionType);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行已审批操作失败: id={} action={}", approvalId, actionType, e);
            throw new RuntimeException("执行已审批操作失败: " + e.getMessage(), e);
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