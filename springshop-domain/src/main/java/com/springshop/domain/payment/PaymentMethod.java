package com.springshop.domain.payment;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 결제 수단을 표현하는 sealed interface.
 *
 * <p>현재 지원되는 결제 수단:
 * <ul>
 *   <li>{@link CreditCard} - 신용/체크 카드, 할부 옵션 포함</li>
 *   <li>{@link BankTransfer} - 계좌 이체</li>
 *   <li>{@link MobilePayment} - 카카오페이/토스페이/네이버페이</li>
 *   <li>{@link VirtualAccount} - 가상계좌(입금 대기 후 자동 확인)</li>
 * </ul>
 *
 * <p>각 결제 수단은 record로 정의되어 불변성을 보장하며, 카드번호/계좌번호는 마스킹된
 * 형태로만 저장된다. PG사 응답에 따라 신규 결제 수단이 추가될 때 sealed 패턴 매칭이
 * 모든 사용처에서 컴파일 타임에 컴플리트니스 검사를 강제한다.</p>
 *
 * @author SpringShop Domain Team
 */
public sealed interface PaymentMethod
        permits PaymentMethod.CreditCard,
                PaymentMethod.BankTransfer,
                PaymentMethod.MobilePayment,
                PaymentMethod.VirtualAccount {

    /**
     * 사용자에게 표시되는 결제 수단 설명.
     */
    String getDisplayName();

    /**
     * 결제 수단 유형 코드(영문).
     */
    String typeCode();

    /**
     * 신용/체크 카드 결제.
     *
     * @param maskedCardNumber 마스킹된 카드 번호 (예: 1234-****-****-5678)
     * @param cardCompany 카드사명 (예: 신한, KB국민, 삼성)
     * @param installmentMonths 할부 개월 수 (0=일시불, 2~24=할부)
     * @param isCredit true=신용카드, false=체크카드
     */
    record CreditCard(
            String maskedCardNumber,
            String cardCompany,
            int installmentMonths,
            boolean isCredit
    ) implements PaymentMethod {

        public CreditCard {
            Objects.requireNonNull(maskedCardNumber, "카드 번호는 null일 수 없습니다");
            Objects.requireNonNull(cardCompany, "카드사명은 null일 수 없습니다");
            if (installmentMonths < 0 || installmentMonths > 24) {
                throw new IllegalArgumentException(
                        "할부 개월 수는 0~24 사이여야 합니다: " + installmentMonths);
            }
            if (!isCredit && installmentMonths > 0) {
                throw new IllegalArgumentException("체크카드는 할부가 불가능합니다");
            }
        }

        @Override
        public String getDisplayName() {
            String type = isCredit ? "신용" : "체크";
            String suffix = installmentMonths == 0 ? "일시불" : (installmentMonths + "개월");
            return "%s %s %s (%s)".formatted(cardCompany, type, maskedCardNumber, suffix);
        }

        @Override
        public String typeCode() {
            return "CREDIT_CARD";
        }
    }

    /**
     * 계좌 이체.
     */
    record BankTransfer(
            String bankName,
            String maskedAccountNumber
    ) implements PaymentMethod {

        public BankTransfer {
            Objects.requireNonNull(bankName, "은행명은 null일 수 없습니다");
            Objects.requireNonNull(maskedAccountNumber, "계좌번호는 null일 수 없습니다");
        }

        @Override
        public String getDisplayName() {
            return "%s %s".formatted(bankName, maskedAccountNumber);
        }

        @Override
        public String typeCode() {
            return "BANK_TRANSFER";
        }
    }

    /**
     * 간편결제 (카카오페이, 토스페이, 네이버페이 등).
     */
    record MobilePayment(
            String provider,
            String maskedPhoneNumber
    ) implements PaymentMethod {

        public MobilePayment {
            Objects.requireNonNull(provider, "간편결제 제공자는 null일 수 없습니다");
            Objects.requireNonNull(maskedPhoneNumber, "전화번호는 null일 수 없습니다");
        }

        @Override
        public String getDisplayName() {
            return "%s (%s)".formatted(provider, maskedPhoneNumber);
        }

        @Override
        public String typeCode() {
            return "MOBILE_PAYMENT";
        }
    }

    /**
     * 가상계좌. 일정 기간 내 입금되어야 결제 완료된다.
     */
    record VirtualAccount(
            String bankName,
            String accountNumber,
            LocalDateTime expiresAt
    ) implements PaymentMethod {

        public VirtualAccount {
            Objects.requireNonNull(bankName, "은행명은 null일 수 없습니다");
            Objects.requireNonNull(accountNumber, "계좌번호는 null일 수 없습니다");
            Objects.requireNonNull(expiresAt, "만료시간은 null일 수 없습니다");
            if (expiresAt.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new IllegalArgumentException("만료시간이 과거입니다: " + expiresAt);
            }
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }

        @Override
        public String getDisplayName() {
            return "%s 가상계좌 %s (만료 %s)".formatted(bankName, accountNumber, expiresAt);
        }

        @Override
        public String typeCode() {
            return "VIRTUAL_ACCOUNT";
        }
    }

    /**
     * 즉시 결제 완료 가능한지 판단. 가상계좌는 입금 대기가 필요하다.
     */
    default boolean isInstant() {
        return switch (this) {
            case CreditCard c -> true;
            case MobilePayment m -> true;
            case BankTransfer b -> true;
            case VirtualAccount v -> false;
        };
    }

    /**
     * 할부 가능 여부.
     */
    default boolean supportsInstallment() {
        return this instanceof CreditCard;
    }
}
