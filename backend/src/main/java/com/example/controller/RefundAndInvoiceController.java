package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.billing.InvoiceService;
import com.example.billing.RefundService;
import com.example.common.Result;
import com.example.entity.Invoice;
import com.example.entity.Subscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 退款与发票管理 — 用户端退款申请、发票开具与查询。
 */
@Tag(name = "退款与发票")
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class RefundAndInvoiceController {

    private final RefundService refundService;
    private final InvoiceService invoiceService;

    // ==================== 退款相关 ====================

    @RateLimit(time = 60, count = 2)
    @NoRepeatSubmit
    @Operation(summary = "申请退款（7天无理由 / 按比例退款）")
    @PostMapping("/refund/apply")
    public Result<Subscription> applyRefund(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "退款原因") @RequestParam String reason) {
        Subscription sub = refundService.applyRefund(userId, reason);
        return Result.success(sub);
    }

    @Operation(summary = "查询退款状态")
    @GetMapping("/refund/status")
    public Result<Subscription> getRefundStatus(@RequestAttribute("userId") Long userId) {
        Subscription sub = refundService.getRefundStatus(userId);
        return Result.success(sub);
    }

    // ==================== 发票相关 ====================

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "申请开具发票")
    @PostMapping("/invoice/apply")
    public Result<Invoice> applyInvoice(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "关联订单号") @RequestParam String orderNo,
            @Parameter(description = "发票类型：personal(个人) / enterprise(企业)") @RequestParam String invoiceType,
            @Parameter(description = "发票抬头") @RequestParam String invoiceTitle,
            @Parameter(description = "税号（企业发票必填）") @RequestParam(required = false) String taxNumber) {
        Invoice invoice = invoiceService.applyInvoice(userId, orderNo, invoiceType, invoiceTitle, taxNumber);
        return Result.success(invoice);
    }

    @Operation(summary = "我的发票列表")
    @GetMapping("/invoice/list")
    public Result<List<Invoice>> invoiceList(@RequestAttribute("userId") Long userId) {
        return Result.success(invoiceService.getUserInvoices(userId));
    }

    @Operation(summary = "发票详情")
    @GetMapping("/invoice/{invoiceId}")
    public Result<Invoice> invoiceDetail(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long invoiceId) {
        return Result.success(invoiceService.getInvoiceById(invoiceId, userId));
    }

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "作废发票")
    @PostMapping("/invoice/{invoiceId}/cancel")
    public Result<?> cancelInvoice(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long invoiceId) {
        invoiceService.cancelInvoice(invoiceId, userId);
        return Result.success();
    }
}