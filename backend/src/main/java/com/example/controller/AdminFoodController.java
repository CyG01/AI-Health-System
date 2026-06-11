package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.FoodItemCreateDTO;
import com.example.dto.FoodItemUpdateDTO;
import com.example.service.AdminApprovalService;
import com.example.service.AuditLogService;
import com.example.service.FoodService;
import com.example.vo.FoodItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理员食物字典管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/food")
public class AdminFoodController {

    @Autowired
    private FoodService foodService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AdminApprovalService approvalService;

    @Operation(summary = "查询所有食物项目")
    @GetMapping("/items")
    public Result<List<FoodItemVO>> listAllItems() {
        return Result.success(foodService.listAllItems());
    }

    @NoRepeatSubmit
    @Operation(summary = "新增食物项目")
    @PostMapping("/item")
    public Result<Void> create(@Validated @RequestBody FoodItemCreateDTO dto,
                               @RequestAttribute("userId") Long userId,
                               HttpServletRequest request) {
        foodService.createFoodItem(dto);
        auditLogService.log(userId, null, "CREATE", "food_item", null,
                "新增食物项目: " + dto.getName(), request.getRemoteAddr());
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "修改食物项目")
    @PutMapping("/item")
    public Result<Void> update(@Validated @RequestBody FoodItemUpdateDTO dto,
                               @RequestAttribute("userId") Long userId,
                               HttpServletRequest request) {
        foodService.updateFoodItem(dto);
        auditLogService.log(userId, null, "UPDATE", "food_item", dto.getId(),
                "修改食物项目: " + dto.getName(), request.getRemoteAddr());
        return Result.success();
    }

    @NoRepeatSubmit
    @Operation(summary = "删除食物项目")
    @DeleteMapping("/item/{id}")
    public Result<?> delete(@PathVariable Long id,
                            @RequestAttribute("userId") Long userId,
                            @RequestHeader(value = "X-Approval-Id", required = false) Long approvalId,
                            HttpServletRequest request) {
        if (!approvalService.checkApproval("delete_food", approvalId, userId)) {
            return Result.error(403, "删除食物项目为敏感操作，请先发起审批申请: POST /api/admin/approvals/request");
        }
        foodService.deleteFoodItem(id);
        auditLogService.log(userId, null, "DELETE", "food_item", id,
                "删除食物项目 [审批ID:" + approvalId + "]", request.getRemoteAddr());
        approvalService.markExecuted(approvalId);
        return Result.success();
    }
}