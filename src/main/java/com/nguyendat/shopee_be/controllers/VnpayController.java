package com.nguyendat.shopee_be.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.nguyendat.shopee_be.dto.VnpayRequest;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.services.OrderService;

@RestController
@RequestMapping("/api/vnpay")
public class VnpayController {

    @Autowired
    private OrderService orderService;

    // Inject URL frontend từ application.properties
    @Value("${frontend.url}")
    private String feBaseUrl;

    // Tạo URL thanh toán VNPAY cho đơn của chính người đang đăng nhập (số tiền lấy từ đơn hàng)
    @PostMapping("/create-payment")
    public ResponseEntity<String> createPayment(@RequestBody VnpayRequest request, @AuthenticationPrincipal User user) {
        String paymentUrl = orderService.createVnpayUrl(request.getOrderId(), user);
        return ResponseEntity.ok(paymentUrl);
    }

    // Callback từ VNPAY sau khi thanh toán xong. Chữ ký sai hoặc số tiền lệch thì đơn không bị thay đổi.
    @GetMapping("/return")
    public RedirectView vnpayReturn(@RequestParam Map<String, String> params) {
        boolean success = orderService.processVnpayReturned(params);

        String redirectUrl = feBaseUrl + "/vnpay-done?" +
                "orderId=" + enc(params.get("vnp_TxnRef")) +
                "&vnp_Amount=" + enc(params.get("vnp_Amount")) +
                "&vnp_BankCode=" + enc(params.get("vnp_BankCode")) +
                "&vnp_CardType=" + enc(params.get("vnp_CardType")) +
                "&vnp_OrderInfo=" + enc(params.get("vnp_OrderInfo")) +
                "&vnp_PayDate=" + enc(params.get("vnp_PayDate")) +
                "&vnp_TransactionNo=" + enc(params.get("vnp_TransactionNo")) +
                "&status=" + (success ? "success" : "fail");

        return new RedirectView(redirectUrl);
    }

    // Tham số thiếu được coi là chuỗi rỗng (tránh lỗi 500 khi gọi không đủ tham số)
    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
