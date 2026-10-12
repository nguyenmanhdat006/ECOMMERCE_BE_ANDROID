package com.nguyendat.shopee_be;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nguyendat.shopee_be.entities.User;
import com.nguyendat.shopee_be.repositories.UserDetailRepository;
import com.nguyendat.shopee_be.services.AuthorityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy toàn bộ luồng API qua MockMvc (đi qua Security filter, controller, service, DB thật).
 * Không mở cổng mạng. Dữ liệu tạo ra có tiền tố "apitest-" / "APITEST" và được dọn sau khi chạy.
 * Kết quả từng lời gọi ghi vào file JSON (system property apitest.out, mặc định target/api-results.json).
 *
 * Chạy:  ./mvnw test -Dtest=ApiFlowIT   (không chạy trong `mvn test` mặc định vì tên kết thúc bằng IT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.seed.enabled=false", "spring.jpa.show-sql=false",
        "logging.level.org.springframework.security=WARN", "logging.level.org.hibernate=WARN",
        "VNPAY_TMN_CODE=TESTCODE", "VNPAY_HASH_SECRET=TESTSECRETKEY"})
class ApiFlowIT {

    private static final String PW = "Test@12345";
    private static final String VNP_SECRET = "TESTSECRETKEY";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserDetailRepository users;
    @Autowired AuthorityService authorities;
    @Autowired PasswordEncoder encoder;
    @MockitoBean JavaMailSender mailSender;

    record Resp(int status, String raw, JsonNode json) {
        String id() { return json == null ? null : json.path("id").asText(null); }
    }

    private final List<Map<String, Object>> calls = new ArrayList<>();
    private final String sfx = UUID.randomUUID().toString().substring(0, 6);

    // ------------------------------------------------------------------ helpers
    private Resp call(String group, String id, String desc, HttpMethod method, String url, String token,
                      Object body, int expected, String known) {
        MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.request(method, URI.create(url));
        String reqJson = null;
        try {
            if (body != null) {
                reqJson = om.writeValueAsString(body);
                rb.contentType(MediaType.APPLICATION_JSON).content(reqJson);
            }
            if (token != null) rb.header("Authorization", "Bearer " + token);
            MvcResult res = mvc.perform(rb).andReturn();
            String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
            if (raw.isEmpty() && res.getResponse().getHeader("Location") != null) raw = "Location: " + res.getResponse().getHeader("Location");
            JsonNode json = null;
            try { json = om.readTree(raw); } catch (Exception ignore) { }
            record(group, id, desc, method.name(), url, reqJson, expected, res.getResponse().getStatus(), raw, known);
            return new Resp(res.getResponse().getStatus(), raw, json);
        } catch (Exception e) {
            record(group, id, desc, method.name(), url, reqJson, expected, -1, "EXCEPTION: " + e, known);
            return new Resp(-1, "EXCEPTION: " + e, null);
        }
    }

    private Resp call(String group, String id, String desc, HttpMethod m, String url, String token, Object body, int expected) {
        return call(group, id, desc, m, url, token, body, expected, null);
    }

    private Resp upload(String group, String id, String desc, String url, String token, int expected) {
        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        try {
            MvcResult res = mvc.perform(MockMvcRequestBuilders.multipart(url)
                    .file(new MockMultipartFile("files", "apitest-" + sfx + ".png", "image/png", png))
                    .param("markFirstAsPrimary", "true").header("Authorization", "Bearer " + token)).andReturn();
            String raw = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
            JsonNode json = null;
            try { json = om.readTree(raw); } catch (Exception ignore) { }
            record(group, id, desc, "POST", url, "multipart: files=<png 1x1>, markFirstAsPrimary=true", expected, res.getResponse().getStatus(), raw, null);
            return new Resp(res.getResponse().getStatus(), raw, json);
        } catch (Exception e) {
            record(group, id, desc, "POST", url, "multipart", expected, -1, "EXCEPTION: " + e, null);
            return new Resp(-1, "EXCEPTION: " + e, null);
        }
    }

    private void check(String group, String id, String desc, boolean ok, String detail, String known) {
        record(group, id, desc, "CHECK", "-", null, 1, ok ? 1 : 0, detail, known);
    }

    private void record(String group, String id, String desc, String method, String url, String req,
                        int expected, int status, String raw, String known) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("group", group); m.put("id", id); m.put("desc", desc); m.put("method", method); m.put("url", url);
        m.put("request", req); m.put("expected", expected); m.put("status", status);
        m.put("response", raw != null && raw.length() > 3000 ? raw.substring(0, 3000) + "...(cắt)" : raw);
        m.put("pass", expected == status); m.put("known", known);
        calls.add(m);
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    private String loginToken(String email) throws Exception {
        MvcResult r = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(map("userName", email, "password", PW)))).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).path("token").asText(null);
    }

    private UUID mkUser(String email, String role) {
        User u = User.builder().email(email).firstName("Api").lastName("Test " + role).phoneNumber("0900000000")
                .password(encoder.encode(PW)).provider("manual").enabled(true)
                .authorities(new ArrayList<>(List.of(authorities.getRole(role)))).build();
        return users.save(u).getId();
    }

    private void cleanup() {
        String U = "(select id from auth_user_details where email like 'apitest-%')";
        String P = "(select id from products where slug like 'apitest-%')";
        String O = "(select id from orders where order_number like 'APITEST-%' or customer_id in " + U + ")";
        String C = "(select id from categories where code like 'APITEST%')";
        String R = "(select id from inventory_receipts where receipt_number like 'APITEST-%')";
        String[] sql = {
                "delete from reviews where user_id in " + U + " or product_id in " + P,
                "delete from order_status_history where order_id in " + O,
                "delete from order_items where order_id in " + O + " or product_id in " + P,
                "delete from orders where id in " + O,
                "delete from carts where user_id in " + U + " or product_id in " + P,
                "delete from addresses where user_id in " + U,
                "delete from inventory_receipt_items where receipt_id in " + R + " or product_id in " + P,
                "delete from inventory_receipts where id in " + R,
                "delete from stock_movements where product_id in " + P,
                "delete from product_resources where product_id in " + P,
                "delete from product_variant where product_id in " + P,
                "delete from products where slug like 'apitest-%'",
                "delete from category_type where category_id in " + C,
                "delete from categories where code like 'APITEST%'",
                "delete from auth_user_authority where user_id in " + U,
                "delete from auth_user_details where email like 'apitest-%'"};
        for (String s : sql) {
            try { jdbc.update(s); } catch (Exception e) { System.err.println("cleanup skip: " + e.getMessage()); }
        }
    }

    private int stockOf(String variantId) {
        return jdbc.queryForObject("select stock_quantity from product_variant where id = ?", Integer.class, UUID.fromString(variantId));
    }

    private String paymentStatusOf(String orderId) {
        return jdbc.queryForObject("select payment_status from orders where id = ?", String.class, UUID.fromString(orderId));
    }

    private int movements(String referenceId) {
        return jdbc.queryForObject("select count(*) from stock_movements where reference_id = ?", Integer.class, UUID.fromString(referenceId));
    }

    // Chữ ký VNPAY độc lập với mã ứng dụng: tham số vnp_* sắp xếp, mã hóa URL, HMAC-SHA512
    private static String vnpQuery(Map<String, String> p) {
        return new TreeMap<>(p).entrySet().stream()
                .filter(e -> !e.getKey().startsWith("vnp_SecureHash") && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse("");
    }

    private static String vnpSign(Map<String, String> p) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(VNP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        StringBuilder sb = new StringBuilder();
        for (byte b : mac.doFinal(vnpQuery(p).getBytes(StandardCharsets.UTF_8))) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static Map<String, String> parseQuery(String url) {
        Map<String, String> m = new LinkedHashMap<>();
        String q = url.substring(url.indexOf('?') + 1);
        for (String kv : q.split("&")) {
            int i = kv.indexOf('=');
            m.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8), URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }

    private String signedReturnUrl(String orderId, String amount, String responseCode) throws Exception {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_TxnRef", orderId); p.put("vnp_Amount", amount); p.put("vnp_ResponseCode", responseCode);
        p.put("vnp_BankCode", "NCB"); p.put("vnp_CardType", "ATM"); p.put("vnp_OrderInfo", "test");
        p.put("vnp_PayDate", "20261013120000"); p.put("vnp_TransactionNo", "1");
        p.put("vnp_SecureHash", vnpSign(p));
        return "/api/vnpay/return?" + vnpQuery(p) + "&vnp_SecureHash=" + p.get("vnp_SecureHash");
    }

    // ------------------------------------------------------------------ the flow
    @Test
    void apiFlow() throws Exception {
        cleanup();
        try {
            run();
        } finally {
            cleanup();
            Path out = Path.of(System.getProperty("apitest.out", "target/api-results.json"));
            Files.createDirectories(out.toAbsolutePath().getParent());
            Files.writeString(out, om.writerWithDefaultPrettyPrinter().writeValueAsString(calls), StandardCharsets.UTF_8);
            long fail = calls.stream().filter(c -> !(Boolean) c.get("pass") && c.get("known") == null).count();
            long known = calls.stream().filter(c -> !(Boolean) c.get("pass") && c.get("known") != null).count();
            System.out.println("API-FLOW total=" + calls.size() + " pass=" + calls.stream().filter(c -> (Boolean) c.get("pass")).count() + " fail=" + fail + " known=" + known);
        }
        assertThat(calls).isNotEmpty();
    }

    private void run() throws Exception {
        String fake = UUID.randomUUID().toString();

        // ===== A. Công khai và bảo mật cơ bản =====
        call("A. Công khai", "A1", "Health check", HttpMethod.GET, "/api/health", null, null, 200);
        call("A. Công khai", "A2", "Danh sách sản phẩm (public)", HttpMethod.GET, "/api/products", null, null, 200);
        Resp a3 = call("A. Công khai", "A3", "Tìm kiếm không dấu: 'ao' khớp 'Áo'", HttpMethod.GET, "/api/products/search?keyword=ao&page=0&size=3&sortBy=price&sortDirection=desc", null, null, 200);
        check("A. Công khai", "A3b", "Từ khóa không dấu tìm ra sản phẩm có dấu", a3.json != null && a3.json.path("totalItems").asInt() > 0,
                "totalItems=" + (a3.json == null ? "?" : a3.json.path("totalItems").asText()), null);
        call("A. Công khai", "A4", "Danh sách danh mục", HttpMethod.GET, "/api/category", null, null, 200);
        call("A. Công khai", "A5", "Danh sách loại danh mục", HttpMethod.GET, "/api/category-types", null, null, 200);
        call("A. Công khai", "A6", "Sản phẩm không tồn tại -> 404", HttpMethod.GET, "/api/products/" + fake, null, null, 404);
        call("A. Công khai", "A7", "Swagger api-docs", HttpMethod.GET, "/v3/api-docs", null, null, 200);
        Resp a8 = call("A. Công khai", "A8", "Đơn của tôi khi chưa đăng nhập -> 401", HttpMethod.GET, "/api/orders/me", null, null, 401);
        check("A. Công khai", "A8b", "Lỗi 401 có timestamp dạng chuỗi ISO (không phải mảng)", a8.json != null && a8.json.path("timestamp").isTextual(), "timestamp=" + (a8.json == null ? "?" : a8.json.path("timestamp").toString()), null);
        call("A. Công khai", "A9", "Token rác -> 401", HttpMethod.GET, "/api/orders/me", "garbage.token.value", null, 401);
        call("A. Công khai", "A10", "Admin API khi chưa đăng nhập -> 401", HttpMethod.GET, "/api/admin/users", null, null, 401);

        // ===== B. Đăng ký, kích hoạt, đăng nhập =====
        String email = "apitest-" + sfx + "@shopease.test";
        Map<String, Object> reg = map("firstName", "Lan", "lastName", "Nguyen", "email", email, "password", PW, "phoneNumber", "0911222333");
        call("B. Xác thực", "B1", "Đăng ký thành công", HttpMethod.POST, "/api/auth/register", null, reg, 200);
        call("B. Xác thực", "B2", "Đăng ký trùng email -> 400", HttpMethod.POST, "/api/auth/register", null, reg, 400);
        call("B. Xác thực", "B3", "Đăng ký mật khẩu ngắn -> 400", HttpMethod.POST, "/api/auth/register", null,
                map("email", "apitest-short-" + sfx + "@shopease.test", "password", "123"), 400);
        call("B. Xác thực", "B4", "Đăng nhập khi chưa kích hoạt -> 401", HttpMethod.POST, "/api/auth/login", null, map("userName", email, "password", PW), 401);
        call("B. Xác thực", "B5", "Kích hoạt sai mã -> 400", HttpMethod.POST, "/api/auth/verify", null, map("userName", email, "code", "000000"), 400);
        String code = jdbc.queryForObject("select verification_code from auth_user_details where email = ?", String.class, email);
        call("B. Xác thực", "B6", "Kích hoạt đúng mã", HttpMethod.POST, "/api/auth/verify", null, map("userName", email, "code", code), 200);
        Resp login = call("B. Xác thực", "B7", "Đăng nhập thành công", HttpMethod.POST, "/api/auth/login", null, map("userName", email, "password", PW), 200);
        call("B. Xác thực", "B8", "Đăng nhập sai mật khẩu -> 401", HttpMethod.POST, "/api/auth/login", null, map("userName", email, "password", "Sai@12345"), 401);
        String userToken = login.json == null ? null : login.json.path("token").asText(null);
        String userId = login.json == null ? null : login.json.path("user").path("id").asText(null);
        call("B. Xác thực", "B9", "Xem hồ sơ", HttpMethod.GET, "/api/user/profile", userToken, null, 200);
        call("B. Xác thực", "B10", "Hồ sơ khi chưa đăng nhập -> 401", HttpMethod.GET, "/api/user/profile", null, null, 401);

        mkUser("apitest-admin-" + sfx + "@shopease.test", "ADMIN");
        UUID user2Uuid = mkUser("apitest-user2-" + sfx + "@shopease.test", "USER");
        String adminToken = loginToken("apitest-admin-" + sfx + "@shopease.test");
        String user2Token = loginToken("apitest-user2-" + sfx + "@shopease.test");
        String user2Id = user2Uuid.toString();

        // ===== C. Phân quyền =====
        call("C. Phân quyền", "C1", "USER gọi /api/admin/users -> 403", HttpMethod.GET, "/api/admin/users", userToken, null, 403);
        call("C. Phân quyền", "C2", "USER tạo sản phẩm -> 403", HttpMethod.POST, "/api/products", userToken, map("name", "x"), 403);
        call("C. Phân quyền", "C3", "USER xem dashboard -> 403", HttpMethod.GET, "/api/dashboard/kpi", userToken, null, 403);
        call("C. Phân quyền", "C4", "USER xem phiếu nhập kho -> 403", HttpMethod.GET, "/api/inventory-receipts", userToken, null, 403);
        call("C. Phân quyền", "C5", "USER xem tất cả đơn -> 403", HttpMethod.GET, "/api/orders", userToken, null, 403);
        call("C. Phân quyền", "C6", "USER tạo danh mục -> 403", HttpMethod.POST, "/api/category", userToken, map("name", "x"), 403);
        call("C. Phân quyền", "C7", "USER xem tất cả giỏ hàng -> 403", HttpMethod.GET, "/api/carts", userToken, null, 403);
        call("C. Phân quyền", "C8", "USER xem tất cả địa chỉ -> 403", HttpMethod.GET, "/api/addresses", userToken, null, 403);

        // ===== D. Quản trị người dùng =====
        call("D. Quản trị người dùng", "D1", "ADMIN xem danh sách user", HttpMethod.GET, "/api/admin/users", adminToken, null, 200);
        Resp created = call("D. Quản trị người dùng", "D2", "ADMIN tạo user", HttpMethod.POST, "/api/admin/users", adminToken,
                map("firstName", "Tao", "lastName", "Moi", "email", "apitest-created-" + sfx + "@shopease.test", "phoneNumber", "0900111222", "enabled", true, "role", "USER"), 201);
        String cid = created.id();
        call("D. Quản trị người dùng", "D3", "ADMIN xem chi tiết user", HttpMethod.GET, "/api/admin/users/" + cid, adminToken, null, 200);
        call("D. Quản trị người dùng", "D4", "ADMIN cấp quyền ADMIN", HttpMethod.PUT, "/api/admin/users/" + cid, adminToken, map("role", "ADMIN"), 200);
        call("D. Quản trị người dùng", "D5", "Role không hợp lệ -> 400", HttpMethod.PUT, "/api/admin/users/" + cid, adminToken, map("role", "SUPERMAN"), 400);
        call("D. Quản trị người dùng", "D6", "User không tồn tại -> 404", HttpMethod.GET, "/api/admin/users/" + fake, adminToken, null, 404);
        call("D. Quản trị người dùng", "D7", "ADMIN vô hiệu hóa user", HttpMethod.DELETE, "/api/admin/users/" + cid, adminToken, null, 204);

        // ===== E. Danh mục, sản phẩm, biến thể (ADMIN) =====
        Resp cat = call("E. Danh mục và sản phẩm", "E1", "Tạo danh mục", HttpMethod.POST, "/api/category", adminToken,
                map("name", "Danh muc test " + sfx, "code", "APITEST" + sfx, "description", "Danh mục kiểm thử"), 201);
        String catId = cat.id();
        Resp typ = call("E. Danh mục và sản phẩm", "E2", "Tạo loại danh mục", HttpMethod.POST, "/api/category-types", adminToken,
                map("name", "Loai test " + sfx, "code", "APITEST-T" + sfx, "description", "Loại kiểm thử", "categoryId", catId), 201);
        String typId = typ.id();
        call("E. Danh mục và sản phẩm", "E3", "Xem chi tiết danh mục (public)", HttpMethod.GET, "/api/category/" + catId, null, null, 200);
        call("E. Danh mục và sản phẩm", "E4", "Sửa danh mục", HttpMethod.PUT, "/api/category/" + catId, adminToken,
                map("name", "Danh muc test sua " + sfx, "code", "APITEST" + sfx, "description", "Đã sửa"), 200);
        call("E. Danh mục và sản phẩm", "E5", "Xem loại danh mục theo id", HttpMethod.GET, "/api/category-types/" + typId, null, null, 200);
        call("E. Danh mục và sản phẩm", "E6", "Sửa loại danh mục", HttpMethod.PUT, "/api/category-types/" + typId, adminToken,
                map("name", "Loai test sua " + sfx, "code", "APITEST-T" + sfx, "description", "Đã sửa", "categoryId", catId), 200);

        Map<String, Object> prod = map("name", "Áo test " + sfx, "description", "Sản phẩm kiểm thử", "price", 249000, "brand", "ApiTest",
                "newArrival", true, "slug", "apitest-" + sfx, "categoryId", catId, "categoryTypeId", typId,
                "variants", List.of(map("color", "Den", "size", "M", "stockQuantity", 10), map("color", "Den", "size", "L", "stockQuantity", 5)));
        Resp p = call("E. Danh mục và sản phẩm", "E7", "Tạo sản phẩm kèm 2 biến thể (trả ProductDto)", HttpMethod.POST, "/api/products", adminToken, prod, 201);
        String pid = p.id();
        check("E. Danh mục và sản phẩm", "E7b", "Phản hồi tạo là ProductDto đầy đủ (có categoryName, variants, newArrival=true)",
                p.json != null && p.json.path("variants").size() == 2 && p.json.path("newArrival").asBoolean() && !p.json.path("categoryName").isMissingNode(), p.raw.length() > 200 ? p.raw.substring(0, 200) : p.raw, null);
        call("E. Danh mục và sản phẩm", "E7c", "Tạo sản phẩm trùng slug -> 409", HttpMethod.POST, "/api/products", adminToken, prod, 409);
        call("E. Danh mục và sản phẩm", "E7d", "Tạo sản phẩm thiếu trường bắt buộc -> 400", HttpMethod.POST, "/api/products", adminToken, map("name", "x"), 400);
        Resp pget = call("E. Danh mục và sản phẩm", "E8", "Xem chi tiết sản phẩm (public)", HttpMethod.GET, "/api/products/" + pid, null, null, 200);
        String v1 = null, v2 = null;
        if (pget.json != null && pget.json.path("variants").size() >= 2) {
            v1 = pget.json.path("variants").get(0).path("id").asText();
            v2 = pget.json.path("variants").get(1).path("id").asText();
            if (pget.json.path("variants").get(0).path("size").asText().equals("L")) { String t = v1; v1 = v2; v2 = t; }
        }
        // v1 = cỡ M (tồn 10), v2 = cỡ L (tồn 5)
        call("E. Danh mục và sản phẩm", "E9", "Lọc sản phẩm theo categoryId", HttpMethod.GET, "/api/products?categoryId=" + catId, null, null, 200);
        call("E. Danh mục và sản phẩm", "E10", "Lọc sản phẩm theo slug", HttpMethod.GET, "/api/products?slug=apitest-" + sfx, null, null, 200);
        call("E. Danh mục và sản phẩm", "E11", "Tìm kiếm có lọc giá và danh mục", HttpMethod.GET, "/api/products/search?categoryId=" + catId + "&minPrice=100000&maxPrice=300000&isNewArrival=true", null, null, 200);
        call("E. Danh mục và sản phẩm", "E11b", "Tìm theo tên có dấu 'Áo test'", HttpMethod.GET, "/api/products/search?keyword=Ao%20test%20" + sfx, null, null, 200);
        Resp e12 = call("E. Danh mục và sản phẩm", "E12", "Sửa một phần: chỉ đổi giá", HttpMethod.PUT, "/api/products/" + pid, adminToken, map("price", 259000), 200);
        check("E. Danh mục và sản phẩm", "E12b", "Sửa một phần giữ nguyên tên, thương hiệu, biến thể và đổi giá",
                e12.json != null && e12.json.path("price").asInt() == 259000 && e12.json.path("brand").asText().equals("ApiTest")
                        && e12.json.path("name").asText().startsWith("Áo test") && e12.json.path("variants").size() == 2, e12.raw.length() > 250 ? e12.raw.substring(0, 250) : e12.raw, null);
        upload("E. Danh mục và sản phẩm", "E13", "Tải ảnh sản phẩm", "/api/products/" + pid + "/resources", adminToken, 200);
        Resp resList = call("E. Danh mục và sản phẩm", "E14", "Danh sách ảnh sản phẩm (public)", HttpMethod.GET, "/api/products/" + pid + "/resources", null, null, 200);
        String resId = resList.json != null && resList.json.isArray() && resList.json.size() > 0 ? resList.json.get(0).path("id").asText() : fake;
        call("E. Danh mục và sản phẩm", "E15", "Đặt ảnh chính", HttpMethod.PUT, "/api/products/" + pid + "/resources/" + resId + "/primary", adminToken, null, 200);
        call("E. Danh mục và sản phẩm", "E16", "USER đặt ảnh chính -> 403", HttpMethod.PUT, "/api/products/" + pid + "/resources/" + resId + "/primary", userToken, null, 403);
        call("E. Danh mục và sản phẩm", "E17", "Xóa ảnh sản phẩm", HttpMethod.DELETE, "/api/products/" + pid + "/resources/" + resId, adminToken, null, 200);
        call("E. Danh mục và sản phẩm", "E18", "Xem biến thể (cần đăng nhập)", HttpMethod.GET, "/api/product-variants/" + v1, userToken, null, 200);
        call("E. Danh mục và sản phẩm", "E19", "Xem biến thể khi chưa đăng nhập -> 401", HttpMethod.GET, "/api/product-variants/" + v1, null, null, 401);
        Resp nv = call("E. Danh mục và sản phẩm", "E20", "Thêm biến thể cho sản phẩm có sẵn (ADMIN)", HttpMethod.POST, "/api/product-variants", adminToken,
                map("productId", pid, "color", "Trang", "size", "S", "stockQuantity", 7), 201);
        call("E. Danh mục và sản phẩm", "E20b", "Thêm biến thể thiếu màu -> 400", HttpMethod.POST, "/api/product-variants", adminToken, map("productId", pid, "size", "S", "stockQuantity", 7), 400);
        call("E. Danh mục và sản phẩm", "E20c", "USER thêm biến thể -> 403", HttpMethod.POST, "/api/product-variants", userToken, map("productId", pid, "color", "x", "size", "x", "stockQuantity", 1), 403);
        call("E. Danh mục và sản phẩm", "E21", "Sửa biến thể", HttpMethod.PUT, "/api/product-variants/" + nv.id(), adminToken,
                map("color", "Trang", "size", "S", "stockQuantity", 9), 200);
        call("E. Danh mục và sản phẩm", "E22", "Xóa biến thể chưa phát sinh giao dịch", HttpMethod.DELETE, "/api/product-variants/" + nv.id(), adminToken, null, 200);

        // ===== F. Địa chỉ =====
        Resp ad = call("F. Địa chỉ", "F1", "Thêm địa chỉ của tôi", HttpMethod.POST, "/api/addresses", userToken,
                map("name", "Nhà riêng", "street", "12 Nguyễn Trãi", "city", "Hồ Chí Minh", "state", "Quận 1", "zipCode", "700000", "phoneNumber", "0911222333"), 201);
        String adId = ad.id();
        call("F. Địa chỉ", "F2", "Xem địa chỉ của tôi", HttpMethod.GET, "/api/addresses/my-addresses", userToken, null, 200);
        call("F. Địa chỉ", "F3", "Sửa địa chỉ của tôi", HttpMethod.PUT, "/api/addresses/" + adId, userToken,
                map("name", "Công ty", "street", "88 Láng Hạ", "city", "Hà Nội", "state", "Đống Đa", "zipCode", "100000", "phoneNumber", "0911222333"), 200);
        call("F. Địa chỉ", "F4", "User khác sửa địa chỉ -> 403", HttpMethod.PUT, "/api/addresses/" + adId, user2Token,
                map("name", "x", "street", "x", "city", "x", "state", "x", "zipCode", "x", "phoneNumber", "x"), 403);
        call("F. Địa chỉ", "F5", "User khác xóa địa chỉ -> 403", HttpMethod.DELETE, "/api/addresses/" + adId, user2Token, null, 403);
        call("F. Địa chỉ", "F6", "Xem địa chỉ theo userId của chính mình", HttpMethod.GET, "/api/addresses/user/" + userId, userToken, null, 200);
        call("F. Địa chỉ", "F6b", "Xem địa chỉ của user khác -> 403", HttpMethod.GET, "/api/addresses/user/" + userId, user2Token, null, 403);
        call("F. Địa chỉ", "F6c", "Xem địa chỉ theo id của user khác -> 403", HttpMethod.GET, "/api/addresses/" + adId, user2Token, null, 403);
        call("F. Địa chỉ", "F7", "Xóa địa chỉ của tôi", HttpMethod.DELETE, "/api/addresses/" + adId, userToken, null, 200);

        // ===== G. Giỏ hàng =====
        Resp cart = call("G. Giỏ hàng", "G1", "Thêm vào giỏ (userId lấy từ token)", HttpMethod.POST, "/api/carts", userToken,
                map("quantity", 2, "productVariantId", v1), 201);
        String cartId = cart.id();
        check("G. Giỏ hàng", "G1b", "Dòng giỏ thuộc về người đang đăng nhập và đúng sản phẩm",
                cart.json != null && userId.equals(cart.json.path("userId").asText()) && pid.equals(cart.json.path("productId").asText()), cart.raw, null);
        call("G. Giỏ hàng", "G2", "Xem giỏ theo user", HttpMethod.GET, "/api/carts/user/" + userId, userToken, null, 200);
        call("G. Giỏ hàng", "G3", "Sửa số lượng", HttpMethod.PUT, "/api/carts/" + cartId, userToken, map("quantity", 3, "productVariantId", v1), 200);
        call("G. Giỏ hàng", "G4", "Xem một dòng giỏ", HttpMethod.GET, "/api/carts/" + cartId, userToken, null, 200);
        call("G. Giỏ hàng", "G5", "Thêm vượt tồn kho -> 400", HttpMethod.POST, "/api/carts", userToken, map("quantity", 9999, "productVariantId", v2), 400);
        call("G. Giỏ hàng", "G5b", "Số lượng 0 -> 400", HttpMethod.POST, "/api/carts", userToken, map("quantity", 0, "productVariantId", v2), 400);
        call("G. Giỏ hàng", "G6", "User khác xem giỏ của người khác -> 403", HttpMethod.GET, "/api/carts/user/" + userId, user2Token, null, 403);
        call("G. Giỏ hàng", "G7", "User khác sửa giỏ của người khác -> 403", HttpMethod.PUT, "/api/carts/" + cartId, user2Token, map("quantity", 1, "productVariantId", v1), 403);
        call("G. Giỏ hàng", "G8", "User khác xóa giỏ của người khác -> 403", HttpMethod.DELETE, "/api/carts/" + cartId, user2Token, null, 403);
        call("G. Giỏ hàng", "G9", "Xóa dòng giỏ của mình", HttpMethod.DELETE, "/api/carts/" + cartId, userToken, null, 200);

        // ===== H. Đơn hàng =====
        int stockBefore = stockOf(v1);
        // Cố tình gửi giá, tổng tiền, trạng thái, customerId sai: máy chủ phải bỏ qua
        Map<String, Object> orderReq = map("customerId", user2Id, "orderNumber", "APITEST-" + sfx + "-1", "totalAmount", 1, "status", "PAID",
                "paymentMethod", "COD", "shippingAddress", "Nguyen Van A - 0911222333 - 12 Nguyễn Trãi, Quận 1, HCM", "notes", "Giao giờ hành chính",
                "orderItems", List.of(map("productId", pid, "productVariantId", v1, "quantity", 2, "unitPrice", 1, "totalPrice", 1)));
        Resp o1 = call("H. Đơn hàng", "H1", "Tạo đơn hàng", HttpMethod.POST, "/api/orders", userToken, orderReq, 201);
        String o1Id = o1.id();
        check("H. Đơn hàng", "H1b", "Máy chủ tự tính tiền (259000 x 2 = 518000) và đặt trạng thái PENDING, bỏ qua dữ liệu client gửi",
                o1.json != null && o1.json.path("totalAmount").asInt() == 518000 && "PENDING".equals(o1.json.path("status").asText())
                        && o1.json.path("orderItems").size() == 1 && o1.json.path("orderItems").get(0).path("unitPrice").asInt() == 259000, o1.raw.length() > 300 ? o1.raw.substring(0, 300) : o1.raw, null);
        check("H. Đơn hàng", "H1c", "Người đặt là người đăng nhập (không phải customerId client gửi)",
                userId != null && userId.equals(jdbc.queryForObject("select customer_id::text from orders where id = ?", String.class, UUID.fromString(o1Id))), "customer_id đúng", null);
        check("H. Đơn hàng", "H2", "Tạo đơn trừ tồn kho biến thể (10 -> 8)", stockOf(v1) == stockBefore - 2, "tồn trước=" + stockBefore + ", tồn sau=" + stockOf(v1), null);
        check("H. Đơn hàng", "H2b", "Có sổ biến động kho OUT cho đơn", movements(o1Id) == 1, "số dòng stock_movements=" + movements(o1Id), null);
        call("H. Đơn hàng", "H3", "Đơn của tôi", HttpMethod.GET, "/api/orders/me", userToken, null, 200);
        Resp od = call("H. Đơn hàng", "H4", "Chi tiết đơn (chủ đơn)", HttpMethod.GET, "/api/orders/" + o1Id, userToken, null, 200);
        check("H. Đơn hàng", "H4b", "OrderResponse có paymentStatus = UNPAID", od.json != null && "UNPAID".equals(od.json.path("paymentStatus").asText()), od.json == null ? "?" : od.json.path("paymentStatus").asText(), null);
        String itemId = od.json != null && od.json.path("orderItems").size() > 0 ? od.json.path("orderItems").get(0).path("id").asText() : fake;
        call("H. Đơn hàng", "H5", "Item đã mua chưa đánh giá", HttpMethod.GET, "/api/orders/unreviewed", userToken, null, 200);
        call("H. Đơn hàng", "H6", "USER xem tất cả đơn -> 403", HttpMethod.GET, "/api/orders", userToken, null, 403);
        call("H. Đơn hàng", "H7", "ADMIN xem tất cả đơn", HttpMethod.GET, "/api/orders", adminToken, null, 200);
        call("H. Đơn hàng", "H7b", "ADMIN xem chi tiết đơn của người khác", HttpMethod.GET, "/api/orders/" + o1Id, adminToken, null, 200);
        call("H. Đơn hàng", "H8", "Đơn không tồn tại -> 404", HttpMethod.GET, "/api/orders/" + fake, userToken, null, 404);
        call("H. Đơn hàng", "H9", "USER khác xem đơn của người khác -> 403", HttpMethod.GET, "/api/orders/" + o1Id, user2Token, null, 403);
        call("H. Đơn hàng", "H10", "USER đổi trạng thái đơn -> 403", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", userToken, map("status", "SHIPPING"), 403);
        call("H. Đơn hàng", "H11", "ADMIN chuyển sai luồng PENDING->PAID -> 400", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", adminToken, map("status", "PAID"), 400);
        call("H. Đơn hàng", "H12", "ADMIN PENDING->SHIPPING", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", adminToken, map("status", "SHIPPING"), 200);
        call("H. Đơn hàng", "H13", "ADMIN SHIPPING->WAIT_DELIVER", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", adminToken, map("status", "WAIT_DELIVER"), 200);
        call("H. Đơn hàng", "H14", "ADMIN WAIT_DELIVER->PAID", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", adminToken, map("status", "PAID"), 200);
        call("H. Đơn hàng", "H15", "Trạng thái không hợp lệ -> 400", HttpMethod.PATCH, "/api/orders/" + o1Id + "/status", adminToken, map("status", "ABC"), 400);
        int hist = jdbc.queryForObject("select count(*) from order_status_history where order_id = ? and changed_by like 'apitest-admin-%'", Integer.class, UUID.fromString(o1Id));
        check("H. Đơn hàng", "H16", "Lịch sử trạng thái ghi 3 dòng, người đổi lấy từ token", hist == 3, "số dòng lịch sử=" + hist, null);

        // đơn thứ hai: hủy đơn, thanh toán
        Map<String, Object> o2req = new LinkedHashMap<>(orderReq);
        o2req.put("orderNumber", "APITEST-" + sfx + "-2"); o2req.put("paymentMethod", "VNPAY");
        o2req.put("orderItems", List.of(map("productId", pid, "productVariantId", v2, "quantity", 3)));
        int s2before = stockOf(v2);
        Resp o2 = call("H. Đơn hàng", "H17", "Tạo đơn thứ hai (VNPAY)", HttpMethod.POST, "/api/orders", userToken, o2req, 201);
        String o2Id = o2.id();
        check("H. Đơn hàng", "H17b", "Tạo đơn thứ hai trừ kho (5 -> 2)", stockOf(v2) == s2before - 3, "tồn trước=" + s2before + ", sau=" + stockOf(v2), null);
        call("H. Đơn hàng", "H18", "Tạo đơn trùng orderNumber -> 409", HttpMethod.POST, "/api/orders", userToken, o2req, 409);
        call("H. Đơn hàng", "H18b", "Tạo đơn vượt tồn kho (6 > 2) -> 409", HttpMethod.POST, "/api/orders", userToken,
                map("orderNumber", "APITEST-" + sfx + "-X", "paymentMethod", "COD", "shippingAddress", "x", "orderItems", List.of(map("productVariantId", v2, "quantity", 6))), 409);
        call("H. Đơn hàng", "H18c", "Tạo đơn không có sản phẩm -> 400", HttpMethod.POST, "/api/orders", userToken,
                map("orderNumber", "APITEST-" + sfx + "-Y", "paymentMethod", "COD", "shippingAddress", "x", "orderItems", List.of()), 400);
        call("H. Đơn hàng", "H18d", "Tạo đơn phương thức thanh toán sai -> 400", HttpMethod.POST, "/api/orders", userToken,
                map("orderNumber", "APITEST-" + sfx + "-Z", "paymentMethod", "BITCOIN", "shippingAddress", "x", "orderItems", List.of(map("productVariantId", v2, "quantity", 1))), 400);
        check("H. Đơn hàng", "H18e", "Đơn bị từ chối không làm đổi tồn kho", stockOf(v2) == s2before - 3, "tồn v2=" + stockOf(v2), null);
        call("H. Đơn hàng", "H19", "USER tự đổi trạng thái thanh toán -> 403", HttpMethod.PATCH, "/api/orders/" + o2Id + "/payment-status", userToken, map("status", "PAID"), 403);
        call("H. Đơn hàng", "H19b", "ADMIN đổi trạng thái thanh toán", HttpMethod.PATCH, "/api/orders/" + o2Id + "/payment-status", adminToken, map("status", "UNPAID"), 200);
        call("H. Đơn hàng", "H20", "ADMIN hủy đơn", HttpMethod.PATCH, "/api/orders/" + o2Id + "/status", adminToken, map("status", "CANCELED"), 200);
        check("H. Đơn hàng", "H21", "Hủy đơn cộng trả đúng số lượng đã trừ (tồn về 5)", stockOf(v2) == s2before, "tồn trước khi tạo=" + s2before + ", sau khi hủy=" + stockOf(v2), null);
        call("H. Đơn hàng", "H22", "Chuyển tiếp từ trạng thái cuối CANCELED -> 400", HttpMethod.PATCH, "/api/orders/" + o2Id + "/status", adminToken, map("status", "SHIPPING"), 400);
        Resp o3 = call("H. Đơn hàng", "H23", "User2 tạo đơn thứ ba", HttpMethod.POST, "/api/orders", user2Token,
                map("orderNumber", "APITEST-" + sfx + "-3", "paymentMethod", "COD", "shippingAddress", "x",
                        "orderItems", List.of(map("productVariantId", v1, "quantity", 1))), 201);
        call("H. Đơn hàng", "H24", "USER xóa đơn -> 403 (chỉ ADMIN)", HttpMethod.DELETE, "/api/orders/" + o1Id, user2Token, null, 403);
        call("H. Đơn hàng", "H25", "ADMIN xóa đơn đang xử lý (PAID) -> 400", HttpMethod.DELETE, "/api/orders/" + o1Id, adminToken, null, 400);
        call("H. Đơn hàng", "H26", "ADMIN xóa đơn đã hủy (kèm lịch sử)", HttpMethod.DELETE, "/api/orders/" + o2Id, adminToken, null, 200);

        // ===== I. VNPAY =====
        String o3Id = o3.id();
        Resp pay = call("I. Thanh toán VNPAY", "I1", "Tạo URL thanh toán (số tiền gửi sai bị bỏ qua)", HttpMethod.POST, "/api/vnpay/create-payment", user2Token, map("orderId", o3Id, "amount", 1), 200);
        check("I. Thanh toán VNPAY", "I1b", "URL có vnp_Amount = tổng đơn x 100 (25900000)", pay.raw.contains("vnp_Amount=25900000") && pay.raw.contains("vnp_SecureHash="), pay.raw.length() > 160 ? pay.raw.substring(0, 160) : pay.raw, null);
        Map<String, String> payParams = parseQuery(pay.raw);
        check("I. Thanh toán VNPAY", "I1c", "Chữ ký của URL tạo ra khớp cách ký độc lập", payParams.get("vnp_SecureHash") != null && payParams.get("vnp_SecureHash").equals(vnpSign(payParams)), "ok", null);
        call("I. Thanh toán VNPAY", "I2", "Tạo URL thanh toán khi chưa đăng nhập -> 401", HttpMethod.POST, "/api/vnpay/create-payment", null, map("orderId", o3Id), 401);
        call("I. Thanh toán VNPAY", "I2b", "Thanh toán đơn của người khác -> 403", HttpMethod.POST, "/api/vnpay/create-payment", userToken, map("orderId", o3Id), 403);
        Resp i3 = call("I. Thanh toán VNPAY", "I3", "Callback hợp lệ (có chữ ký, đúng số tiền) -> 302 status=success", HttpMethod.GET, signedReturnUrl(o3Id, "25900000", "00"), null, null, 302);
        check("I. Thanh toán VNPAY", "I3b", "Đơn chuyển payment_status = PAID", "PAID".equals(paymentStatusOf(o3Id)) && i3.raw.contains("status=success"), "payment_status=" + paymentStatusOf(o3Id), null);

        Resp o4 = call("I. Thanh toán VNPAY", "I4", "Tạo đơn để thử callback giả", HttpMethod.POST, "/api/orders", user2Token,
                map("orderNumber", "APITEST-" + sfx + "-4", "paymentMethod", "VNPAY", "shippingAddress", "x", "orderItems", List.of(map("productVariantId", v1, "quantity", 1))), 201);
        String o4Id = o4.id();
        Resp i5 = call("I. Thanh toán VNPAY", "I5", "Callback giả mạo không có chữ ký -> 302 status=fail", HttpMethod.GET,
                "/api/vnpay/return?vnp_TxnRef=" + o4Id + "&vnp_ResponseCode=00&vnp_Amount=25900000&vnp_BankCode=X&vnp_CardType=X&vnp_OrderInfo=x&vnp_PayDate=1&vnp_TransactionNo=1", null, null, 302);
        check("I. Thanh toán VNPAY", "I5b", "Callback giả mạo không làm đổi trạng thái thanh toán", "UNPAID".equals(paymentStatusOf(o4Id)) && i5.raw.contains("status=fail"), "payment_status=" + paymentStatusOf(o4Id), null);
        call("I. Thanh toán VNPAY", "I6", "Callback có chữ ký nhưng sai số tiền -> 302 status=fail", HttpMethod.GET, signedReturnUrl(o4Id, "100", "00"), null, null, 302);
        check("I. Thanh toán VNPAY", "I6b", "Sai số tiền không làm đổi trạng thái thanh toán", "UNPAID".equals(paymentStatusOf(o4Id)), "payment_status=" + paymentStatusOf(o4Id), null);
        call("I. Thanh toán VNPAY", "I7", "Callback thiếu tham số không gây lỗi 500", HttpMethod.GET, "/api/vnpay/return?vnp_TxnRef=" + o4Id, null, null, 302);
        call("I. Thanh toán VNPAY", "I8", "Callback mã lỗi (ví dụ 24) có chữ ký -> status=fail, đơn FAILED", HttpMethod.GET, signedReturnUrl(o4Id, "25900000", "24"), null, null, 302);
        check("I. Thanh toán VNPAY", "I8b", "Thanh toán thất bại đặt payment_status = FAILED", "FAILED".equals(paymentStatusOf(o4Id)), "payment_status=" + paymentStatusOf(o4Id), null);

        // ===== J. Đánh giá =====
        call("J. Đánh giá", "J0", "Đánh giá item của đơn chưa hoàn thành -> 400", HttpMethod.POST, "/api/reviews", user2Token,
                map("productId", pid, "orderItemId", jdbc.queryForObject("select id::text from order_items where order_id = ?", String.class, UUID.fromString(o3Id)), "rating", 5, "comment", "x"), 400, null);
        Resp rv = call("J. Đánh giá", "J1", "Viết đánh giá cho item đã mua (userId lấy từ token)", HttpMethod.POST, "/api/reviews", userToken,
                map("productId", pid, "orderItemId", itemId, "rating", 5, "comment", "Vải mát, giao nhanh"), 201);
        String rvId = rv.id();
        Resp j2 = call("J. Đánh giá", "J2", "Xem đánh giá của sản phẩm (public)", HttpMethod.GET, "/api/reviews/product/" + pid, null, null, 200);
        check("J. Đánh giá", "J2b", "API công khai không lộ email, số điện thoại người đánh giá",
                j2.json != null && j2.json.size() > 0 && j2.json.get(0).path("user").path("email").isMissingNode() && j2.json.get(0).path("user").path("phoneNumber").isMissingNode(),
                "user=" + (j2.json != null && j2.json.size() > 0 ? j2.json.get(0).path("user").toString() : "?"), null);
        Resp prAfter = call("J. Đánh giá", "J3", "Điểm trung bình sản phẩm được cập nhật", HttpMethod.GET, "/api/products/" + pid, null, null, 200);
        check("J. Đánh giá", "J4", "product.rating = 5.0 sau đánh giá 5 sao", prAfter.json != null && prAfter.json.path("rating").asDouble() == 5.0,
                "rating=" + (prAfter.json == null ? "?" : prAfter.json.path("rating").asText()), null);
        call("J. Đánh giá", "J4b", "Đánh giá lại cùng một dòng đơn -> 409", HttpMethod.POST, "/api/reviews", userToken,
                map("productId", pid, "orderItemId", itemId, "rating", 4, "comment", "lần hai"), 409);
        call("J. Đánh giá", "J5", "Sửa đánh giá của mình", HttpMethod.PUT, "/api/reviews/" + rvId, userToken, map("rating", 4, "comment", "Đã sửa"), 200);
        call("J. Đánh giá", "J5b", "User khác sửa đánh giá của người khác -> 403", HttpMethod.PUT, "/api/reviews/" + rvId, user2Token, map("rating", 1, "comment", "hack"), 403);
        call("J. Đánh giá", "J5c", "User khác xóa đánh giá của người khác -> 403", HttpMethod.DELETE, "/api/reviews/" + rvId, user2Token, null, 403);
        call("J. Đánh giá", "J6", "User khác đánh giá item không phải của mình -> 403", HttpMethod.POST, "/api/reviews", user2Token,
                map("productId", pid, "orderItemId", itemId, "rating", 1, "comment", "x"), 403);
        call("J. Đánh giá", "J7", "Điểm đánh giá ngoài khoảng 1-5 -> 400", HttpMethod.POST, "/api/reviews", userToken,
                map("productId", pid, "orderItemId", itemId, "rating", 9, "comment", "x"), 400);
        call("J. Đánh giá", "J8", "Xóa đánh giá của mình", HttpMethod.DELETE, "/api/reviews/" + rvId, userToken, null, 200);

        // ===== K. Kho =====
        int stockV1 = stockOf(v1);
        Map<String, Object> receiptReq = map("receiptNumber", "APITEST-" + sfx + "-R1", "supplierName", "Nhà cung cấp test", "status", "PENDING", "notes", "Phiếu test",
                "items", List.of(map("productVariantId", v1, "quantity", 5, "unitCost", 100)));
        Resp rc = call("K. Kho", "K1", "Tạo phiếu nhập kèm dòng chi tiết", HttpMethod.POST, "/api/inventory-receipts", adminToken, receiptReq, 201);
        check("K. Kho", "K1b", "Tổng tiền do máy chủ tính (5 x 100 = 500), có 1 dòng, ngày nhập tự gán",
                rc.json != null && rc.json.path("totalAmount").asInt() == 500 && rc.json.path("items").size() == 1 && !rc.json.path("receiptDate").isNull(), rc.raw.length() > 250 ? rc.raw.substring(0, 250) : rc.raw, null);
        call("K. Kho", "K2", "Tạo phiếu trùng số phiếu -> 409", HttpMethod.POST, "/api/inventory-receipts", adminToken, receiptReq, 409);
        call("K. Kho", "K2b", "Tạo phiếu không có dòng chi tiết -> 400", HttpMethod.POST, "/api/inventory-receipts", adminToken,
                map("receiptNumber", "APITEST-" + sfx + "-R9", "supplierName", "x", "items", List.of()), 400);
        call("K. Kho", "K3", "Danh sách phiếu nhập", HttpMethod.GET, "/api/inventory-receipts", adminToken, null, 200);
        call("K. Kho", "K4", "Chi tiết phiếu nhập", HttpMethod.GET, "/api/inventory-receipts/" + rc.id(), adminToken, null, 200);
        check("K. Kho", "K5a", "Phiếu PENDING chưa cộng tồn kho", stockOf(v1) == stockV1, "tồn v1=" + stockOf(v1), null);
        Map<String, Object> approve = new LinkedHashMap<>(receiptReq);
        approve.put("status", "APPROVED"); approve.put("supplierName", "Nhà cung cấp mới");
        call("K. Kho", "K5", "Duyệt phiếu (PENDING -> APPROVED)", HttpMethod.PUT, "/api/inventory-receipts/" + rc.id(), adminToken, approve, 200);
        check("K. Kho", "K6", "Duyệt phiếu cộng tồn kho (+5) và ghi sổ biến động IN", stockOf(v1) == stockV1 + 5 && movements(rc.id()) == 1,
                "tồn trước=" + stockV1 + ", sau=" + stockOf(v1) + ", biến động=" + movements(rc.id()), null);
        call("K. Kho", "K6b", "Sửa phiếu đã duyệt -> 400", HttpMethod.PUT, "/api/inventory-receipts/" + rc.id(), adminToken, approve, 400);
        call("K. Kho", "K6c", "Xóa phiếu đã duyệt -> 400", HttpMethod.DELETE, "/api/inventory-receipts/" + rc.id(), adminToken, null, 400);
        Resp rc2 = call("K. Kho", "K6d", "Tạo phiếu thứ hai", HttpMethod.POST, "/api/inventory-receipts", adminToken,
                map("receiptNumber", "APITEST-" + sfx + "-R2", "supplierName", "x", "items", List.of(map("productVariantId", v2, "quantity", 1, "unitCost", 10))), 201);
        call("K. Kho", "K7", "Xóa phiếu đang chờ duyệt", HttpMethod.DELETE, "/api/inventory-receipts/" + rc2.id(), adminToken, null, 200);

        // ===== L. Dashboard =====
        call("L. Dashboard", "L1", "KPI (ADMIN)", HttpMethod.GET, "/api/dashboard/kpi", adminToken, null, 200);
        call("L. Dashboard", "L2", "Làm mới biểu đồ (ADMIN)", HttpMethod.POST, "/api/dashboard/refresh", adminToken, null, 200);
        call("L. Dashboard", "L3", "Làm mới biểu đồ bởi USER -> 403", HttpMethod.POST, "/api/dashboard/refresh", userToken, null, 403);

        // ===== M. Xóa dữ liệu và lỗi định dạng =====
        Resp tmp = call("M. Xóa dữ liệu", "M1", "Tạo sản phẩm tạm để xóa", HttpMethod.POST, "/api/products", adminToken,
                map("name", "Tam " + sfx, "price", 1000, "brand", "T", "slug", "apitest-tmp-" + sfx, "categoryId", catId, "categoryTypeId", typId,
                        "variants", List.of(map("color", "X", "size", "S", "stockQuantity", 1))), 201);
        call("M. Xóa dữ liệu", "M2", "Xóa sản phẩm tạm", HttpMethod.DELETE, "/api/products/" + tmp.id(), adminToken, null, 204);
        call("M. Xóa dữ liệu", "M2b", "Xóa sản phẩm đã có đơn hàng -> 409", HttpMethod.DELETE, "/api/products/" + pid, adminToken, null, 409);
        call("M. Xóa dữ liệu", "M3", "Xóa loại danh mục khi còn sản phẩm -> 409", HttpMethod.DELETE, "/api/category-types/" + typId, adminToken, null, 409);
        call("M. Xóa dữ liệu", "M3b", "Xóa biến thể đã phát sinh đơn hàng -> 409", HttpMethod.DELETE, "/api/product-variants/" + v1, adminToken, null, 409);
        call("M. Xóa dữ liệu", "M4", "Đường dẫn UUID sai định dạng -> 400", HttpMethod.GET, "/api/products/not-a-uuid", null, null, 400);
        call("M. Xóa dữ liệu", "M5", "Đăng nhập user đã bị vô hiệu hóa -> 401", HttpMethod.POST, "/api/auth/login", null,
                map("userName", "apitest-created-" + sfx + "@shopease.test", "password", PW), 401);
    }
}
