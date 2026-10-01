package com.nguyendat.shopee_be.config;

import com.nguyendat.shopee_be.entities.*;
import com.nguyendat.shopee_be.repositories.*;
import com.nguyendat.shopee_be.services.AuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

// Tạo dữ liệu mẫu để test. Chỉ chạy khi app.seed.enabled=true (SEED_ENABLED=true trong .env).
// Chạy lại nhiều lần không bị trùng: user đã có thì bỏ qua, catalog chỉ tạo khi bảng categories trống.
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private final UserDetailRepository userRepository;
    private final AuthorityService authorityService;
    private final PasswordEncoder passwordEncoder;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;

    @Value("${app.seed.password}")
    private String seedPassword;

    public DataSeeder(UserDetailRepository userRepository, AuthorityService authorityService,
                      PasswordEncoder passwordEncoder, AddressRepository addressRepository,
                      CategoryRepository categoryRepository, ProductRepository productRepository,
                      CartRepository cartRepository, OrderRepository orderRepository,
                      OrderItemRepository orderItemRepository, OrderStatusHistoryRepository statusHistoryRepository,
                      ReviewRepository reviewRepository, InventoryReceiptRepository inventoryReceiptRepository) {
        this.userRepository = userRepository;
        this.authorityService = authorityService;
        this.passwordEncoder = passwordEncoder;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.reviewRepository = reviewRepository;
        this.inventoryReceiptRepository = inventoryReceiptRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            throw new IllegalStateException("SEED_ENABLED=true nhưng chưa đặt SEED_PASSWORD");
        }
        User admin = seedUser("admin@shopease.test", "Admin", "ShopEase", "0900000001", Role.ADMIN);
        User customer = seedUser("user@shopease.test", "Nguyen", "Van A", "0900000002", Role.USER);
        User customer2 = seedUser("user2@shopease.test", "Tran", "Thi B", "0900000003", Role.USER);

        if (categoryRepository.count() > 0) {
            log.info("[seed] Catalog đã có dữ liệu, bỏ qua seed catalog/đơn hàng");
            return;
        }

        Map<String, Product> products = seedCatalog();
        seedAddresses(customer, customer2);
        seedCarts(customer, products);
        seedOrders(admin, customer, customer2, products);
        seedInventory(products);
        log.info("[seed] Hoàn tất: {} sản phẩm, {} đơn hàng", productRepository.count(), orderRepository.count());
    }

    // ---------- users ----------

    private User seedUser(String email, String firstName, String lastName, String phone, String role) {
        User existing = userRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        Date now = new Date();
        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phone)
                .password(passwordEncoder.encode(seedPassword))
                .provider("manual")
                .enabled(true)
                .authorities(new ArrayList<>(List.of(authorityService.getRole(role))))
                .createdOn(now)
                .updatedOn(now)
                .build();
        log.info("[seed] Tạo user {} ({})", email, role);
        return userRepository.save(user);
    }

    private void seedAddresses(User customer, User customer2) {
        addressRepository.saveAll(List.of(
                Address.builder().user(customer).name("Nhà riêng").street("12 Nguyễn Trãi, P. Bến Thành")
                        .city("Hồ Chí Minh").state("Quận 1").zipCode("700000").phoneNumber("0900000002").build(),
                Address.builder().user(customer).name("Công ty").street("88 Láng Hạ")
                        .city("Hà Nội").state("Đống Đa").zipCode("100000").phoneNumber("0900000002").build(),
                Address.builder().user(customer2).name("Nhà riêng").street("5 Trần Phú")
                        .city("Đà Nẵng").state("Hải Châu").zipCode("550000").phoneNumber("0900000003").build()));
    }

    // ---------- catalog ----------

    private Map<String, Product> seedCatalog() {
        Category men = category("Thời trang nam", "MEN", "Quần áo dành cho nam",
                type("Áo thun", "MEN_TSHIRT"), type("Áo sơ mi", "MEN_SHIRT"), type("Quần jeans", "MEN_JEANS"));
        Category women = category("Thời trang nữ", "WOMEN", "Quần áo dành cho nữ",
                type("Váy", "WOMEN_DRESS"), type("Áo kiểu", "WOMEN_BLOUSE"));
        Category acc = category("Phụ kiện", "ACCESSORIES", "Túi, mũ và phụ kiện",
                type("Túi xách", "ACC_BAG"), type("Mũ", "ACC_HAT"));
        categoryRepository.saveAll(List.of(men, women, acc));

        Map<String, Product> p = new LinkedHashMap<>();
        p.put("tee", product("Áo thun cotton basic", "ao-thun-cotton-basic", "Coolmate", 199000, true, men, 0,
                "Áo thun 100% cotton, thoáng mát, form regular.", new String[]{"Trắng", "Đen"}, new String[]{"M", "L", "XL"}, 30));
        p.put("polo", product("Áo polo pique", "ao-polo-pique", "Routine", 349000, false, men, 0,
                "Áo polo vải cá sấu, cổ dệt bo.", new String[]{"Xanh navy", "Trắng"}, new String[]{"M", "L"}, 20));
        p.put("shirt", product("Áo sơ mi Oxford dài tay", "ao-so-mi-oxford", "Owen", 459000, false, men, 1,
                "Sơ mi Oxford chống nhăn, phù hợp đi làm.", new String[]{"Xanh nhạt", "Trắng"}, new String[]{"39", "40", "41"}, 15));
        p.put("jeans", product("Quần jeans slim fit", "quan-jeans-slim-fit", "Levi's", 899000, true, men, 2,
                "Jeans co giãn nhẹ, ống côn.", new String[]{"Xanh đậm"}, new String[]{"29", "30", "31", "32"}, 12));
        p.put("dress", product("Váy hoa nhí dáng xòe", "vay-hoa-nhi-dang-xoe", "Ivy Moda", 650000, true, women, 0,
                "Váy voan hoa nhí, có lớp lót.", new String[]{"Hồng", "Vàng"}, new String[]{"S", "M", "L"}, 18));
        p.put("maxi", product("Đầm maxi đi biển", "dam-maxi-di-bien", "Elise", 790000, false, women, 0,
                "Đầm maxi chất lụa, dây rút eo.", new String[]{"Trắng"}, new String[]{"S", "M"}, 8));
        p.put("blouse", product("Áo kiểu cổ nơ", "ao-kieu-co-no", "Ivy Moda", 420000, false, women, 1,
                "Áo lụa cổ thắt nơ, tay phồng.", new String[]{"Kem", "Đen"}, new String[]{"S", "M", "L"}, 14));
        p.put("tote", product("Túi tote canvas", "tui-tote-canvas", "Vascara", 280000, false, acc, 0,
                "Túi vải canvas dày, có ngăn khóa kéo.", new String[]{"Be", "Đen"}, new String[]{"Free size"}, 40));
        p.put("cap", product("Mũ lưỡi trai thêu logo", "mu-luoi-trai-theu-logo", "MLB", 550000, true, acc, 1,
                "Mũ kaki, khóa điều chỉnh phía sau.", new String[]{"Đen", "Trắng", "Xanh"}, new String[]{"Free size"}, 25));
        p.put("bucket", product("Mũ bucket vải dù", "mu-bucket-vai-du", "Coolmate", 159000, false, acc, 1,
                "Mũ bucket chống nước nhẹ. Hiện đang hết hàng để test.", new String[]{"Rêu"}, new String[]{"Free size"}, 0));
        productRepository.saveAll(p.values());
        return p;
    }

    private Category category(String name, String code, String description, CategoryType... types) {
        Category category = Category.builder().name(name).code(code).description(description).build();
        for (CategoryType t : types) {
            t.setCategory(category);
        }
        category.setCategoryTypes(new ArrayList<>(List.of(types)));
        return category;
    }

    private CategoryType type(String name, String code) {
        return CategoryType.builder().name(name).code(code).description(name).build();
    }

    private Product product(String name, String slug, String brand, long price, boolean isNew, Category category,
                            int typeIndex, String description, String[] colors, String[] sizes, int stockPerVariant) {
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .brand(brand)
                .price(BigDecimal.valueOf(price))
                .isNewArrival(isNew)
                .rating(0f)
                .description(description)
                .category(category)
                .categoryType(category.getCategoryTypes().get(typeIndex))
                .build();

        List<ProductVariant> variants = new ArrayList<>();
        for (String color : colors) {
            for (String size : sizes) {
                variants.add(ProductVariant.builder().product(product).color(color).size(size)
                        .stockQuantity(stockPerVariant).build());
            }
        }
        product.setProductVariants(variants);

        String text = slug.replace('-', '+');
        product.setResources(new ArrayList<>(List.of(
                Resources.builder().product(product).name(slug + "-1.png").type("image").isPrimary(true)
                        .url("https://placehold.co/600x800/png?text=" + text).build(),
                Resources.builder().product(product).name(slug + "-2.png").type("image").isPrimary(false)
                        .url("https://placehold.co/600x800/png?text=" + text + "+2").build())));
        return product;
    }

    private ProductVariant variant(Product product, int index) {
        return product.getProductVariants().get(index);
    }

    // ---------- cart ----------

    private void seedCarts(User customer, Map<String, Product> p) {
        cartRepository.saveAll(List.of(
                Cart.builder().user(customer).product(p.get("jeans")).productVariant(variant(p.get("jeans"), 1)).quantity(1).build(),
                Cart.builder().user(customer).product(p.get("cap")).productVariant(variant(p.get("cap"), 0)).quantity(2).build()));
    }

    // ---------- orders ----------

    private void seedOrders(User admin, User customer, User customer2, Map<String, Product> p) {
        String addr1 = "Nguyen Van A - 0900000002 - 12 Nguyễn Trãi, P. Bến Thành, Quận 1, Hồ Chí Minh";
        String addr2 = "Tran Thi B - 0900000003 - 5 Trần Phú, Hải Châu, Đà Nẵng";

        // Đơn đã hoàn thành + đã thanh toán, có 1 đánh giá
        Order done = order("SE-SEED-0001", customer, OrderStatus.PAID, PaymentStatus.PAID, "VNPAY", addr1,
                item(p.get("tee"), 0, 2), item(p.get("shirt"), 1, 1));
        history(done, admin, OrderStatus.PENDING, OrderStatus.SHIPPING, OrderStatus.WAIT_DELIVER, OrderStatus.PAID);
        OrderItem reviewed = done.getOrderItems().get(0);
        reviewed.setIsReviewed(true);
        orderItemRepository.save(reviewed);
        reviewRepository.save(Review.builder().user(customer).product(p.get("tee")).orderItem(reviewed)
                .rating(5).comment("Vải mát, mặc rất thoải mái, giao hàng nhanh.").build());
        Product tee = p.get("tee");
        tee.setRating(5f);
        productRepository.save(tee);

        // Đơn chờ xác nhận, COD
        order("SE-SEED-0002", customer, OrderStatus.PENDING, PaymentStatus.UNPAID, "COD", addr1,
                item(p.get("dress"), 1, 1));

        // Đơn đang giao của user2
        Order shipping = order("SE-SEED-0003", customer2, OrderStatus.SHIPPING, PaymentStatus.UNPAID, "COD", addr2,
                item(p.get("tote"), 0, 1), item(p.get("cap"), 2, 1));
        history(shipping, admin, OrderStatus.PENDING, OrderStatus.SHIPPING);

        // Đơn đã hủy
        Order canceled = order("SE-SEED-0004", customer2, OrderStatus.CANCELED, PaymentStatus.UNPAID, "COD", addr2,
                item(p.get("polo"), 0, 1));
        history(canceled, admin, OrderStatus.PENDING, OrderStatus.CANCELED);
    }

    private OrderItem item(Product product, int variantIndex, int quantity) {
        BigDecimal unit = product.getPrice();
        return OrderItem.builder()
                .product(product)
                .productVariant(variant(product, variantIndex))
                .quantity(quantity)
                .unitPrice(unit)
                .totalPrice(unit.multiply(BigDecimal.valueOf(quantity)))
                .isReviewed(false)
                .build();
    }

    private Order order(String number, User customer, OrderStatus status, PaymentStatus paymentStatus,
                        String paymentMethod, String address, OrderItem... items) {
        BigDecimal total = Arrays.stream(items).map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = orderRepository.save(Order.builder()
                .orderNumber(number)
                .customer(customer)
                .status(status)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .shippingAddress(address)
                .totalAmount(total)
                .notes("Dữ liệu mẫu")
                .updatedAt(new Date())
                .build());
        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setOrderItems(orderItemRepository.saveAll(List.of(items)));
        return order;
    }

    // Ghi lịch sử theo chuỗi trạng thái liên tiếp, vd PENDING -> SHIPPING -> PAID
    private void history(Order order, User admin, OrderStatus... chain) {
        for (int i = 1; i < chain.length; i++) {
            OrderStatusHistory h = new OrderStatusHistory();
            h.setOrder(order);
            h.setOldStatus(chain[i - 1]);
            h.setNewStatus(chain[i]);
            h.setChangedBy(admin.getEmail());
            statusHistoryRepository.save(h);
        }
    }

    // ---------- inventory ----------

    private void seedInventory(Map<String, Product> p) {
        InventoryReceipt receipt = InventoryReceipt.builder()
                .receiptNumber("PN-SEED-0001")
                .supplierName("Công ty May Việt Tiến")
                .status("APPROVED")
                .notes("Phiếu nhập mẫu")
                .build();
        List<InventoryReceiptItem> items = List.of(
                receiptItem(receipt, p.get("tee"), 0, 50, 90000),
                receiptItem(receipt, p.get("jeans"), 0, 20, 450000));
        receipt.setInventoryReceiptItems(new ArrayList<>(items));
        receipt.setTotalAmount(items.stream().map(InventoryReceiptItem::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add));
        inventoryReceiptRepository.save(receipt);
    }

    private InventoryReceiptItem receiptItem(InventoryReceipt receipt, Product product, int variantIndex, int qty, long cost) {
        BigDecimal unit = BigDecimal.valueOf(cost);
        return InventoryReceiptItem.builder()
                .inventoryReceipt(receipt)
                .product(product)
                .productVariant(variant(product, variantIndex))
                .quantity(qty)
                .unitCost(unit)
                .totalCost(unit.multiply(BigDecimal.valueOf(qty)))
                .build();
    }
}
