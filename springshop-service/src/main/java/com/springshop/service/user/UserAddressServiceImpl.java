package com.springshop.service.user;

import com.springshop.domain.user.User;
import com.springshop.domain.user.UserAddress;
import com.springshop.domain.user.UserAddressRepository;
import com.springshop.domain.user.UserRepository;
import com.springshop.domain.vo.Address;
import com.springshop.domain.vo.PhoneNumber;
import com.springshop.common.exception.InvalidStateException;
import com.springshop.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 주소 관리 보조 구현체.
 *
 * <p>{@link UserAddressService}의 {@code Impl} 내부 구현은 기본 CRUD를 담당한다.
 * 이 보조 구현체는 운영 관점의 추가 기능 — 일괄 마이그레이션, 통계, 검증 헬퍼 —
 * 를 제공한다. 빈 이름이 충돌하지 않도록 {@code "userAddressServiceExtension"}으로
 * 명시한다.</p>
 *
 * <p>주된 책임:
 * <ul>
 *   <li>주소 추가 시 사용자별 최대 개수(10개) 검증</li>
 *   <li>기본 주소 자동 전환 (기존 default 해제 → 새 default 지정)</li>
 *   <li>지역(시/도) 통계 집계</li>
 *   <li>주소 정합성 검증 (필수 필드, 우편번호 형식)</li>
 * </ul>
 *
 * @see UserAddressService.Impl 표준 구현 (내부 Impl)
 */
@Slf4j
@Service("userAddressServiceExtension")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAddressServiceImpl {

    /**
     * 사용자당 등록 가능한 최대 주소 수.
     */
    public static final int MAX_ADDRESSES_PER_USER = 10;

    /**
     * 우편번호 정규식 (한국 5자리).
     */
    private static final String POSTAL_CODE_PATTERN = "\\d{5}";

    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * 주소 추가. (요구사항: 최대 10개, isDefault 처리)
     */
    @Transactional
    public UserAddress addAddress(Long userId,
                                  String recipientName,
                                  String phone,
                                  String zipCode,
                                  String street,
                                  String city,
                                  String province,
                                  boolean isDefault) {
        log.info("주소 추가 요청: userId={}, isDefault={}", userId, isDefault);
        validatePostalCode(zipCode);
        validateNotBlank("수령인", recipientName);
        validateNotBlank("연락처", phone);
        validateNotBlank("도로명/지번", street);

        long existing = addressRepository.countByUserId(userId);
        if (existing >= MAX_ADDRESSES_PER_USER) {
            throw new InvalidStateException(
                "사용자당 주소 등록 한도 초과: " + MAX_ADDRESSES_PER_USER + "개");
        }

        if (isDefault) {
            clearDefaultAddressFor(userId);
        }

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        var addressVo = new Address(zipCode, street,
            city != null ? city : "N/A", province != null ? province : "N/A", "KR", null);
        var phoneVo = phone != null ? new PhoneNumber(phone) : null;
        var address = UserAddress.create(user, recipientName, addressVo, recipientName, phoneVo);
        if (isDefault || existing == 0) {
            address.setAsDefault();
        }
        var saved = addressRepository.save(address);
        log.info("주소 추가 완료: addressId={}, default={}", saved.getId(), saved.isDefaultAddress());
        return saved;
    }

    /**
     * 주소 수정.
     */
    @Transactional
    public UserAddress updateAddress(Long userId,
                                     Long addressId,
                                     String recipientName,
                                     String phone,
                                     String zipCode,
                                     String street,
                                     String city,
                                     String province,
                                     boolean isDefault) {
        var address = loadOwned(userId, addressId);
        validatePostalCode(zipCode);
        address.update(
            recipientName, phone, zipCode, street, null, city, province, "KR", null
        );
        if (isDefault && !address.isDefaultAddress()) {
            clearDefaultAddressFor(userId);
            address.setAsDefault();
        }
        log.info("주소 수정 완료: addressId={}", addressId);
        return addressRepository.save(address);
    }

    /**
     * 주소 삭제. 기본 주소를 삭제하면 가장 최근 주소가 자동으로 기본으로 승격된다.
     */
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        var address = loadOwned(userId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        log.info("주소 삭제: addressId={}, wasDefault={}", addressId, wasDefault);
        if (wasDefault) {
            promoteNewDefaultIfPossible(userId);
        }
    }

    /**
     * 기본 주소 지정.
     */
    @Transactional
    public UserAddress setDefaultAddress(Long userId, Long addressId) {
        var address = loadOwned(userId, addressId);
        clearDefaultAddressFor(userId);
        address.setAsDefault();
        var saved = addressRepository.save(address);
        log.info("기본 주소 변경: userId={}, addressId={}", userId, addressId);
        return saved;
    }

    /**
     * 사용자 주소 전체 조회. 기본 주소가 최상단에 위치한다.
     */
    public List<UserAddress> findByUserId(Long userId) {
        return addressRepository.findAllByUserId(userId).stream()
            .sorted(Comparator.comparing(UserAddress::isDefaultAddress).reversed()
                .thenComparing(Comparator.comparing(UserAddress::getCreatedAt).reversed()))
            .toList();
    }

    /**
     * 사용자의 기본 주소.
     */
    public Optional<UserAddress> findDefaultAddress(Long userId) {
        return addressRepository.findAllByUserId(userId).stream()
            .filter(UserAddress::isDefaultAddress)
            .findFirst();
    }

    /**
     * 사용자가 등록한 주소의 시/도별 분포.
     *
     * @return Key: 시/도명, Value: 개수
     */
    public Map<String, Long> countByProvince(Long userId) {
        return addressRepository.findAllByUserId(userId).stream()
            .filter(addr -> addr.getState() != null)
            .collect(Collectors.groupingBy(UserAddress::getState, Collectors.counting()));
    }

    /**
     * 사용자가 N일 이상 사용하지 않은 주소 ID 목록.
     */
    public List<Long> findInactiveAddressIds(Long userId, int days) {
        var cutoff = LocalDateTime.now().minusDays(days);
        return addressRepository.findAllByUserId(userId).stream()
            .filter(addr -> addr.getUpdatedAt() == null || addr.getUpdatedAt().isBefore(cutoff))
            .filter(addr -> !addr.isDefaultAddress())
            .map(UserAddress::getId)
            .toList();
    }

    /**
     * 주소 검증 (외부 공개용 헬퍼).
     */
    public boolean isValid(Long userId, Long addressId) {
        return addressRepository.findById(addressId)
            .filter(a -> a.getUserId().equals(userId))
            .isPresent();
    }

    /**
     * 주소 정보 마스킹 (개인정보 보호용).
     */
    public String maskedDisplay(Long addressId) {
        var address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("주소를 찾을 수 없습니다: " + addressId));
        String name = address.getRecipientName();
        String maskedName = name == null || name.length() < 2
            ? "***"
            : name.charAt(0) + "*".repeat(Math.max(1, name.length() - 1));
        String phone = address.getPhoneNumber();
        String maskedPhone = phone == null ? "***" : phone.replaceAll("(\\d{3,4})-(\\d{4})$", "$1-****");
        return "%s | %s | %s %s".formatted(maskedName, maskedPhone, address.getCity(), address.getAddressLine1());
    }

    // ---- helpers ----

    private UserAddress loadOwned(Long userId, Long addressId) {
        var address = addressRepository.findById(addressId)
            .orElseThrow(() -> new ResourceNotFoundException("주소를 찾을 수 없습니다: " + addressId));
        if (!address.getUserId().equals(userId)) {
            throw new InvalidStateException("타 사용자의 주소에는 접근할 수 없습니다.");
        }
        return address;
    }

    private void clearDefaultAddressFor(Long userId) {
        addressRepository.findAllByUserId(userId).stream()
            .filter(UserAddress::isDefaultAddress)
            .forEach(a -> {
                a.unsetDefault();
                addressRepository.save(a);
            });
    }

    private void promoteNewDefaultIfPossible(Long userId) {
        addressRepository.findAllByUserId(userId).stream()
            .max(Comparator.comparing(UserAddress::getCreatedAt))
            .ifPresent(next -> {
                next.setAsDefault();
                addressRepository.save(next);
                log.info("새 기본 주소 자동 지정: addressId={}", next.getId());
            });
    }

    private void validatePostalCode(String zipCode) {
        if (zipCode == null || !zipCode.matches(POSTAL_CODE_PATTERN)) {
            throw new InvalidStateException("우편번호 형식 오류 (5자리 숫자): " + zipCode);
        }
    }

    private void validateNotBlank(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidStateException(fieldName + "은(는) 필수입니다.");
        }
    }
}
