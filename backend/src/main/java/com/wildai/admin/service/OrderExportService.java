package com.wildai.admin.service;

import com.wildai.order.domain.SubscriptionOrder;
import com.wildai.order.repository.SubscriptionOrderRepository;
import com.wildai.order.service.OrderQueryService;
import com.wildai.product.repository.AiServiceProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderExportService {

    private final SubscriptionOrderRepository orderRepo;
    private final AiServiceProductRepository productRepo;

    public OrderExportService(SubscriptionOrderRepository orderRepo, AiServiceProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
    }

    public String exportCsv(List<SubscriptionOrder> orders) {
        var names = productRepo.findAll().stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        StringBuilder sb = new StringBuilder("orderNo,product,amount,orderStatus,paymentStatus,createdAt\n");
        for (SubscriptionOrder o : orders) {
            sb.append(o.getOrderNo()).append(',')
                    .append(names.getOrDefault(o.getProductId(), "-")).append(',')
                    .append(o.getAmount()).append(',')
                    .append(o.getOrderStatus()).append(',')
                    .append(o.getPaymentStatus()).append(',')
                    .append(o.getCreatedAt()).append('\n');
        }
        return sb.toString();
    }
}
