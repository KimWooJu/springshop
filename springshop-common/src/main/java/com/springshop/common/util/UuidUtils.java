package com.springshop.common.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UUID 및 도메인 ID 생성 유틸리티.
 */
public final class UuidUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final AtomicInteger ORDER_SEQUENCE = new AtomicInteger(0);
    private static final AtomicInteger COUPON_SEQUENCE = new AtomicInteger(0);
    private static final DateTimeFormatter ORDER_PREFIX = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String COUPON_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private UuidUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    public static String generateNoHyphens() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 22자 Base64URL 단축 UUID.
     */
    public static String generateShort() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }

    /**
     * 주문번호 생성: ORD-{yyyyMMdd}-{6자리 시퀀스}-{4자리 랜덤}.
     */
    public static String generateOrderNumber() {
        String datePart = LocalDateTime.now().format(ORDER_PREFIX);
        int seq = ORDER_SEQUENCE.incrementAndGet() % 1_000_000;
        int random = RANDOM.nextInt(10_000);
        return "ORD-%s-%06d-%04d".formatted(datePart, seq, random);
    }

    /**
     * 쿠폰 코드 생성: 대문자+숫자 8자 (혼동 문자 제외).
     */
    public static String generateCouponCode() {
        return generateCouponCode(8);
    }

    public static String generateCouponCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(COUPON_CHARSET.charAt(RANDOM.nextInt(COUPON_CHARSET.length())));
        }
        return sb.toString();
    }

    /**
     * 결제번호 생성: PAY-{yyyyMMdd}-{6자리 시퀀스}.
     */
    public static String generatePaymentNumber() {
        String datePart = LocalDateTime.now().format(ORDER_PREFIX);
        int seq = ORDER_SEQUENCE.incrementAndGet() % 1_000_000;
        return "PAY-%s-%06d".formatted(datePart, seq);
    }

    public static boolean isValid(String uuid) {
        if (uuid == null) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static UUID fromString(String uuid) {
        if (!isValid(uuid)) return null;
        return UUID.fromString(uuid);
    }

    /**
     * 거래 추적 ID (correlation ID) 생성.
     */
    public static String generateCorrelationId() {
        return generateShort();
    }
}
