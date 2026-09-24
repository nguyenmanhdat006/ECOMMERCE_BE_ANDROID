package com.nguyendat.shopee_be.ultil;

import java.security.SecureRandom;

public class VerificationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    // Mã 6 chữ số
    public static String generateCode(){
        int code = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
