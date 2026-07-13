package com.wildai.admin.service;

import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.payment.dto.PaymentAmountSnapshot;
import com.wildai.payment.service.PaymentAmountService;
import com.wildai.product.repository.AiServiceProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderExportService {

    private final AiServiceProductRepository productRepo;
    private final PaymentAmountService paymentAmountService;

    public OrderExportService(AiServiceProductRepository productRepo, PaymentAmountService paymentAmountService) {
        this.productRepo = productRepo;
        this.paymentAmountService = paymentAmountService;
    }

    public String exportCsv(List<SubscriptionOrder> orders) {
        var names = productRepo.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        StringBuilder sb = new StringBuilder("orderNo,product,orderAmount,orderCurrency,paidAmount,paidCurrency,exchangeRate,orderStatus,paymentStatus,createdAt\n");
        for (SubscriptionOrder o : orders) {
            PaymentAmountSnapshot amount = paymentAmountService.resolve(o);
            appendRow(sb,
                    o.getOrderNo(),
                    names.getOrDefault(o.getProductId(), "-"),
                    o.getAmount(),
                    o.getCurrency(),
                    amount.paidAmount(),
                    amount.paidCurrency(),
                    amount.exchangeRate(),
                    o.getOrderStatus(),
                    o.getPaymentStatus(),
                    o.getCreatedAt());
        }
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, Object... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(csvValue(values[i]));
        }
        sb.append('\n');
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
