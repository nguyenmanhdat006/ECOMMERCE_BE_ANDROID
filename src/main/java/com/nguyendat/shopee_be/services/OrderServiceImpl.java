package com.nguyendat.shopee_be.services;

import com.nguyendat.shopee_be.dto.OrderItemRequest;
import com.nguyendat.shopee_be.dto.OrderRequest;
import com.nguyendat.shopee_be.entities.Order;
import com.nguyendat.shopee_be.entities.OrderItem;
import com.nguyendat.shopee_be.entities.OrderStatus;
import com.nguyendat.shopee_be.entities.OrderStatusHistory;
import com.nguyendat.shopee_be.entities.PaymentStatus;
import com.nguyendat.shopee_be.entities.Product;
import com.nguyendat.shopee_be.entities.ProductVariant;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.exceptions.ConflictException;
import com.nguyendat.shopee_be.exceptions.ResourceNotFoundEx;
import com.nguyendat.shopee_be.repositories.OrderItemRepository;
import com.nguyendat.shopee_be.repositories.OrderRepository;
import com.nguyendat.shopee_be.repositories.OrderStatusHistoryRepository;
import com.nguyendat.shopee_be.ultil.AccessUtil;
import com.nguyendat.shopee_be.ultil.VnpayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final Set<String> PAYMENT_METHODS = Set.of("COD", "VNPAY");

    @Value("${VNPAY_URL:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnpUrl;

    @Value("${VNPAY_TMN_CODE:}")
    private String vnpTmnCode;

    @Value("${VNPAY_HASH_SECRET:}")
    private String vnpHashSecret;

    @Value("${VNPAY_RETURN_URL_BASE:http://localhost:5173}")
    private String vnpReturnUrlBase;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private StockService stockService;

    @Override
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Override
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundEx("Order not found with id: " + id));
    }

    /**
     * Tạo đơn: người đặt lấy từ token; giá và tổng tiền do máy chủ tính từ giá sản phẩm;
     * tồn kho bị trừ ngay trong cùng giao dịch (không đủ hàng thì cả đơn bị từ chối, 409).
     */
    @Override
    @Transactional
    public Order create(OrderRequest request, User customer) {
        if (request.getOrderItems() == null || request.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một sản phẩm");
        }
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ giao hàng là bắt buộc");
        }
        String paymentMethod = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? "COD" : request.getPaymentMethod().trim().toUpperCase();
        if (!PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod() + " (chỉ COD hoặc VNPAY)");
        }

        String orderNumber = request.getOrderNumber() == null || request.getOrderNumber().isBlank()
                ? "SE-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase()
                : request.getOrderNumber().trim();
        if (orderRepository.existsByOrderNumber(orderNumber)) {
            throw new ConflictException("Mã đơn hàng đã tồn tại: " + orderNumber);
        }

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setOrderDate(new Date());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setShippingAddress(request.getShippingAddress().trim());
        order.setNotes(request.getNotes());
        order.setCustomer(customer);
        Order saved = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getOrderItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            if (itemReq.getProductVariantId() == null) {
                throw new IllegalArgumentException("productVariantId là bắt buộc");
            }
            ProductVariant variant = stockService.lock(itemReq.getProductVariantId());
            Product product = variant.getProduct();
            if (itemReq.getProductId() != null && !itemReq.getProductId().equals(product.getId())) {
                throw new IllegalArgumentException("Biến thể " + variant.getId() + " không thuộc sản phẩm " + itemReq.getProductId());
            }

            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setOrder(saved);
            item.setProduct(product);
            item.setProductVariant(variant);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setTotalPrice(lineTotal);
            items.add(orderItemRepository.save(item));

            stockService.remove(variant, itemReq.getQuantity(), saved.getId(), "ORDER", "Trừ kho khi đặt đơn " + orderNumber);
        }

        saved.setOrderItems(items);
        saved.setTotalAmount(total);
        Order finalOrder = orderRepository.save(saved);

        try {
            dashboardService.pushNewOrderEvent(finalOrder.getId().toString(), buildCustomerName(customer), total.doubleValue());
        } catch (Exception e) {
            log.warn("Không đẩy được sự kiện NEW_ORDER lên dashboard: {}", e.getMessage());
        }
        return finalOrder;
    }

    /** Chỉ xóa được đơn đã hủy hoặc đã hoàn trả (kho đã được cộng trả); xóa kèm lịch sử trạng thái. */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        Order order = findById(id);
        if (order.getStatus() != OrderStatus.CANCELED && order.getStatus() != OrderStatus.REFUND) {
            throw new IllegalArgumentException("Chỉ xóa được đơn đã hủy hoặc đã hoàn trả");
        }
        statusHistoryRepository.deleteByOrder_Id(id);
        orderRepository.delete(order);
    }

    @Override
    public String createVnpayUrl(UUID orderId, User requester) {
        if (vnpTmnCode == null || vnpTmnCode.isBlank() || vnpHashSecret == null || vnpHashSecret.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình VNPAY_TMN_CODE và VNPAY_HASH_SECRET");
        }
        Order order = findById(orderId);
        AccessUtil.requireOwnerOrAdmin(requester, order.getCustomer().getId());
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Đơn hàng đã được thanh toán");
        }
        if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUND) {
            throw new IllegalArgumentException("Không thanh toán được đơn đã hủy hoặc đã hoàn trả");
        }

        // Số tiền lấy từ đơn hàng, không tin số tiền do client gửi
        BigDecimal amount = order.getTotalAmount();

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId.toString());
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrlBase + "/vnpay-done/" + orderId);
        vnpParams.put("vnp_IpAddr", "127.0.0.1");

        String query = VnpayUtil.buildQuery(vnpParams);
        String secureHash = VnpayUtil.hmacSHA512(vnpHashSecret, query);
        return vnpUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /**
     * Xử lý kết quả VNPAY trả về. Chỉ cập nhật đơn khi chữ ký hợp lệ và số tiền khớp với đơn;
     * mọi trường hợp khác trả false và không thay đổi dữ liệu.
     */
    @Override
    @Transactional
    public boolean processVnpayReturned(Map<String, String> params) {
        if (!VnpayUtil.verify(params, vnpHashSecret)) {
            log.warn("Callback VNPAY sai chữ ký hoặc thiếu chữ ký, bỏ qua (vnp_TxnRef={})", params.get("vnp_TxnRef"));
            return false;
        }

        Order order;
        try {
            order = orderRepository.findById(UUID.fromString(params.get("vnp_TxnRef"))).orElse(null);
        } catch (Exception e) {
            return false;
        }
        if (order == null) {
            return false;
        }

        try {
            BigDecimal expected = order.getTotalAmount().multiply(BigDecimal.valueOf(100));
            if (new BigDecimal(params.get("vnp_Amount")).compareTo(expected) != 0) {
                log.warn("Callback VNPAY sai số tiền cho đơn {}: nhận {}, cần {}", order.getId(), params.get("vnp_Amount"), expected);
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return success; // đã ghi nhận trước đó, không đổi trạng thái
        }

        order.setPaymentStatus(success ? PaymentStatus.PAID : PaymentStatus.FAILED);
        order.setUpdatedAt(new Date());
        orderRepository.save(order);

        if (success && order.getStatus() == OrderStatus.PAID) {
            pushRevenueQuietly();
        }
        return success;
    }

    @Override
    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus, String changedBy) {
        Order order = findById(orderId);
        OrderStatus oldStatus = order.getStatus();

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalArgumentException("Không thể chuyển từ trạng thái " + oldStatus + " sang " + newStatus);
        }

        // Hủy hoặc hoàn trả: cộng trả kho (kho đã bị trừ lúc đặt đơn)
        if (newStatus == OrderStatus.CANCELED || newStatus == OrderStatus.REFUND) {
            for (OrderItem item : order.getOrderItems()) {
                ProductVariant variant = stockService.lock(item.getProductVariant().getId());
                stockService.add(variant, item.getQuantity(), order.getId(), "ORDER",
                        "Cộng trả kho do đơn " + order.getOrderNumber() + " chuyển sang " + newStatus);
            }
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        statusHistoryRepository.save(history);

        order.setStatus(newStatus);
        order.setUpdatedAt(new Date());
        Order savedOrder = orderRepository.save(order);

        try {
            dashboardService.pushOrderStatusChanged(orderId.toString(), oldStatus.name(), newStatus.name());
            if (newStatus == OrderStatus.PAID) {
                dashboardService.pushRevenueUpdate();
            }
        } catch (Exception e) {
            log.warn("Không đẩy được sự kiện ORDER_STATUS_CHANGED lên dashboard: {}", e.getMessage());
        }
        return savedOrder;
    }

    @Override
    @Transactional
    public Order updatePaymentStatus(UUID orderId, PaymentStatus newStatus, String changedBy) {
        Order order = findById(orderId);
        order.setPaymentStatus(newStatus);
        order.setUpdatedAt(new Date());
        Order saved = orderRepository.save(order);

        if (newStatus == PaymentStatus.PAID && order.getStatus() == OrderStatus.PAID) {
            pushRevenueQuietly();
        }
        return saved;
    }

    @Override
    public List<Order> findByUser(User user) {
        return orderRepository.findByCustomer(user);
    }

    @Override
    public List<OrderItem> findUnreviewedOrderItemsByUser(User user) {
        if (user == null || user.getId() == null) return java.util.Collections.emptyList();
        return orderItemRepository.findUnreviewedByCustomerId(user.getId());
    }

    private void pushRevenueQuietly() {
        try {
            dashboardService.pushRevenueUpdate();
        } catch (Exception e) {
            log.warn("Không đẩy được sự kiện doanh thu lên dashboard: {}", e.getMessage());
        }
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case PENDING -> List.of(OrderStatus.SHIPPING, OrderStatus.CANCELED).contains(next);
            case SHIPPING -> List.of(OrderStatus.WAIT_DELIVER, OrderStatus.CANCELED).contains(next);
            case WAIT_DELIVER -> List.of(OrderStatus.PAID, OrderStatus.REFUND).contains(next);
            case PAID -> next == OrderStatus.REFUND;
            case CANCELED, REFUND -> false;
        };
    }

    private String buildCustomerName(User customer) {
        if (customer.getFirstName() != null && customer.getLastName() != null) {
            return customer.getFirstName() + " " + customer.getLastName();
        } else if (customer.getFirstName() != null) {
            return customer.getFirstName();
        } else if (customer.getLastName() != null) {
            return customer.getLastName();
        }
        return customer.getEmail();
    }
}
