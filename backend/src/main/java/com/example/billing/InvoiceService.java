package com.example.billing;

import com.example.entity.Invoice;
import com.example.entity.Subscription;
import com.example.mapper.InvoiceMapper;
import com.example.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 发票服务。
 * 支持个人发票和企业发票的开具与查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceMapper invoiceMapper;
    private final SubscriptionMapper subscriptionMapper;

    /**
     * 申请开具发票。
     *
     * @param userId       用户ID
     * @param orderNo      关联订单号
     * @param invoiceType  发票类型：personal / enterprise
     * @param invoiceTitle 发票抬头
     * @param taxNumber    税号（企业发票必填）
     * @return 创建的发票记录
     */
    @Transactional
    public Invoice applyInvoice(Long userId, String orderNo, String invoiceType,
                                 String invoiceTitle, String taxNumber) {
        // 验证订单存在且属于该用户
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getUserId, userId)
                .eq(Subscription::getOrderNo, orderNo);
        Subscription sub = subscriptionMapper.selectOne(wrapper);
        if (sub == null) {
            throw new IllegalArgumentException("订单不存在或不属于当前用户");
        }

        // 检查是否已开票
        if (sub.getOrderNo() != null && hasInvoiceForOrder(orderNo)) {
            throw new IllegalArgumentException("该订单已开具发票，请勿重复申请");
        }

        if ("enterprise".equals(invoiceType) && (taxNumber == null || taxNumber.isBlank())) {
            throw new IllegalArgumentException("企业发票必须填写税号");
        }

        // 计算发票金额
        BigDecimal amount = getInvoiceAmount(sub);

        String invoiceNo = generateInvoiceNo();

        Invoice invoice = new Invoice();
        invoice.setUserId(userId);
        invoice.setInvoiceNo(invoiceNo);
        invoice.setOrderNo(orderNo);
        invoice.setAmount(amount);
        invoice.setInvoiceType(invoiceType);
        invoice.setInvoiceTitle(invoiceTitle);
        invoice.setTaxNumber(taxNumber);
        invoice.setStatus("ISSUED");
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setCreatedAt(LocalDateTime.now());

        invoiceMapper.insert(invoice);

        log.info("发票已开具 userId={} invoiceNo={} amount={} type={}", userId, invoiceNo, amount, invoiceType);
        return invoice;
    }

    /**
     * 查询用户的发票列表。
     */
    public List<Invoice> getUserInvoices(Long userId) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getUserId, userId)
                .orderByDesc(Invoice::getIssueDate);
        return invoiceMapper.selectList(wrapper);
    }

    /**
     * 查询单张发票详情。
     */
    public Invoice getInvoiceById(Long invoiceId, Long userId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null || !invoice.getUserId().equals(userId)) {
            throw new IllegalArgumentException("发票不存在或不属于当前用户");
        }
        return invoice;
    }

    /**
     * 作废发票。
     */
    @Transactional
    public void cancelInvoice(Long invoiceId, Long userId) {
        Invoice invoice = invoiceMapper.selectById(invoiceId);
        if (invoice == null || !invoice.getUserId().equals(userId)) {
            throw new IllegalArgumentException("发票不存在或不属于当前用户");
        }
        if ("CANCELLED".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("发票已作废");
        }
        invoice.setStatus("CANCELLED");
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceMapper.updateById(invoice);
        log.info("发票已作废 invoiceNo={}", invoice.getInvoiceNo());
    }

    /**
     * 检查订单是否已开票。
     */
    private boolean hasInvoiceForOrder(String orderNo) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getOrderNo, orderNo)
                .ne(Invoice::getStatus, "CANCELLED");
        return invoiceMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取发票金额（优先使用自定义价格）。
     */
    private BigDecimal getInvoiceAmount(Subscription sub) {
        if (sub.getCustomPrice() != null && sub.getCustomPrice().compareTo(BigDecimal.ZERO) > 0) {
            return sub.getCustomPrice();
        }
        return switch (sub.getTier()) {
            case "pro" -> new BigDecimal("19.00");
            case "enterprise" -> new BigDecimal("99.00");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * 生成发票编号：INV + 日期 + 随机后缀。
     */
    private String generateInvoiceNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "INV" + dateStr + random;
    }
}