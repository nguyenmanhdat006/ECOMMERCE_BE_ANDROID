package com.nguyendat.shopee_be.controllers;

import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.nguyendat.shopee_be.dto.VnpayRequest;
import com.nguyendat.shopee_be.services.OrderService;

@RestController
@RequestMapping("/api/vnpay")
public class VnpayController {

    @Autowired
    private OrderService orderService;

    // Inject URL frontend từ application.properties
    @Value("${frontend.url}")
    private String feBaseUrl;

    // Tạo URL thanh toán VNPAY
    @PostMapping("/create-payment")
    public ResponseEntity<String> createPayment(@RequestBody VnpayRequest request) {
        String paymentUrl = orderService.createVnpayUrl(request.getOrderId(), request.getAmount());
        return ResponseEntity.ok(paymentUrl);
    }

    // Callback từ VNPAY sau khi thanh toán xong
    @GetMapping("/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        boolean success = orderService.processVnpayReturned(params);

        String orderId = params.get("vnp_TxnRef");
        String amount = params.get("vnp_Amount");
        String bankCode = params.get("vnp_BankCode");
        String cardType = params.get("vnp_CardType");
        String orderInfo = params.get("vnp_OrderInfo");
        String payDate = params.get("vnp_PayDate");
        String transactionNo = params.get("vnp_TransactionNo");

        String status = success ? "success" : "fail";

        // Build URL redirect về FE
        String redirectUrl = feBaseUrl + "/vnpay-done?" +
                "orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8) +
                "&vnp_Amount=" + URLEncoder.encode(amount, StandardCharsets.UTF_8) +
                "&vnp_BankCode=" + URLEncoder.encode(bankCode, StandardCharsets.UTF_8) +
                "&vnp_CardType=" + URLEncoder.encode(cardType, StandardCharsets.UTF_8) +
                "&vnp_OrderInfo=" + URLEncoder.encode(orderInfo, StandardCharsets.UTF_8) +
                "&vnp_PayDate=" + URLEncoder.encode(payDate, StandardCharsets.UTF_8) +
                "&vnp_TransactionNo=" + URLEncoder.encode(transactionNo, StandardCharsets.UTF_8) +
                "&status=" + status;

        return new RedirectView(redirectUrl);
    }
}
