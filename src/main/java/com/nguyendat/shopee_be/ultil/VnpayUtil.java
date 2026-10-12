package com.nguyendat.shopee_be.ultil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VnpayUtil {

    private static final String HASH_KEY = "vnp_SecureHash";
    private static final String HASH_TYPE_KEY = "vnp_SecureHashType";

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);

            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký VNPAY (HMAC-SHA512)", e);
        }
    }

    /**
     * Chuỗi dữ liệu cần ký: các tham số vnp_* khác rỗng, sắp theo tên, giá trị mã hóa URL (UTF-8),
     * không gồm vnp_SecureHash và vnp_SecureHashType.
     */
    public static String buildQuery(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        return sorted.entrySet().stream()
                .filter(e -> !HASH_KEY.equals(e.getKey()) && !HASH_TYPE_KEY.equals(e.getKey()))
                .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    public static String sign(String secret, Map<String, String> params) {
        return hmacSHA512(secret, buildQuery(params));
    }

    /** Kiểm tra vnp_SecureHash trong tham số khớp với chữ ký tính lại bằng khóa bí mật. */
    public static boolean verify(Map<String, String> params, String secret) {
        if (secret == null || secret.isEmpty()) {
            return false;
        }
        String received = params.get(HASH_KEY);
        if (received == null || received.isEmpty()) {
            return false;
        }
        byte[] expected = sign(secret, params).getBytes(StandardCharsets.UTF_8);
        byte[] actual = received.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
