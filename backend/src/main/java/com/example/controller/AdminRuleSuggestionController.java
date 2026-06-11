package com.example.controller;

import com.example.common.Result;
import com.example.entity.RuleSuggestion;
import com.example.service.AuditLogService;
import com.example.service.SafetyRuleIterationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员 - 规则建议管理接口。
 * 查看、审批或拒绝来自采样分析的规则建议。
 */
@RestController
@RequestMapping("/api/admin/rule-suggestions")
@RequiredArgsConstructor
public class AdminRuleSuggestionController {

    private final SafetyRuleIterationService iterationService;
    private final AuditLogService auditLogService;

    /**
     * 获取所有待审批的规则建议。
     */
    @GetMapping("/pending")
    public Result<List<RuleSuggestion>> getPendingSuggestions() {
        List<RuleSuggestion> list = iterationService.getPendingSuggestions();
        return Result.success(list);
    }

    /**
     * 审批并应用规则建议。
     */
    @PostMapping("/{id}/approve")
    public Result<String> approveSuggestion(@PathVariable Long id,
                                             @RequestParam String reviewerName,
                                             @RequestHeader("X-Admin-Id") Long adminId) {
        String msg = iterationService.approveSuggestion(id, reviewerName);
        auditLogService.log(adminId, reviewerName, "APPROVE_RULE_SUGGESTION",
                "rule_suggestion", id, "审批规则建议: result=" + msg, null);
        return Result.success(msg);
    }

    /**
     * 拒绝规则建议。
     */
    @PostMapping("/{id}/reject")
    public Result<String> rejectSuggestion(@PathVariable Long id,
                                            @RequestParam String reviewerName,
                                            @RequestHeader("X-Admin-Id") Long adminId) {
        String msg = iterationService.rejectSuggestion(id, reviewerName);
        auditLogService.log(adminId, reviewerName, "REJECT_RULE_SUGGESTION",
                "rule_suggestion", id, "拒绝规则建议", null);
        return Result.success(msg);
    }
}