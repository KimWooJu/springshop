package com.springshop.service.user;

import com.springshop.domain.user.UserAddress;
import com.springshop.domain.user.UserAddressRepository;
import com.springshop.domain.common.exception.InvalidStateException;
import com.springshop.domain.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 배송 주소 관리 서비스.
 *
 * <p>한 사용자는 최대 10개의 주소를 등록할 수 있으며,
 * 기본 주소는 단 하나만 존재할 수 있다.
 */
public interface UserAddressService {

    UserAddress addAddress(Long userId, AddressCommand command);

    UserAddress updateAddress(Long userId, Long addressId, AddressCommand command);

    void deleteAddress(Long userId, Long addressId);

    UserAddress setDefault(Long userId, Long addressId);

    Optional<UserAddress> findDefault(Long userId);

    List<UserAddress> listAll(Long userId);

    long count(Long userId);

    /** 검증: 주소 소유자가 사용자와 일치하는지 확인 */
    void assertOwnership(Long userId, Long addressId);

    record AddressCommand(
        String recipientName,
        String phoneNumber,
        String postalCode,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String deliveryNote,
        boolean asDefault
    ) {}

    /**
     * 표준 구현.
     */
    @Slf4j
    @Service
    @RequiredArgsConstructor
    class Impl implements UserAddressService {

        private static final int MAX_ADDRESS_PER_USER = 10;

        private final UserAddressRepository addressRepository;

        @Override
        @Transactional
        public UserAddress addAddress(Long userId, AddressCommand cmd) {
            long current = addressRepository.countByUserId(userId);
            if (current >= MAX_ADDRESS_PER_USER) {
                throw new InvalidStateException(
                    "주소는 최대 " + MAX_ADDRESS_PER_USER + "개까지 등록 가능합니다."
                );
            }
            validate(cmd);

            var address = UserAddress.create(
                userId,
                cmd.recipientName(),
                cmd.phoneNumber(),
                cmd.postalCode(),
                cmd.addressLine1(),
                cmd.addressLine2(),
                cmd.city(),
                cmd.state(),
                cmd.country(),
                cmd.deliveryNote()
            );

            // 첫 주소는 자동으로 기본 주소
            if (current == 0 || cmd.asDefault()) {
                unsetCurrentDefault(userId);
                address.setAsDefault();
            }

            var saved = addressRepository.save(address);
            log.info("주소 추가: userId={}, addressId={}, default={}",
                userId, saved.getId(), saved.isDefaultAddress());
            return saved;
        }

        @Override
        @Transactional
        public UserAddress updateAddress(Long userId, Long addressId, AddressCommand cmd) {
            var address = loadOwned(userId, addressId);
            validate(cmd);
            address.update(
                cmd.recipientName(),
                cmd.phoneNumber(),
                cmd.postalCode(),
                cmd.addressLine1(),
                cmd.addressLine2(),
                cmd.city(),
                cmd.state(),
                cmd.country(),
                cmd.deliveryNote()
            );
            if (cmd.asDefault() && !address.isDefaultAddress()) {
                unsetCurrentDefault(userId);
                address.setAsDefault();
            }
            return addressRepository.save(address);
        }

        @Override
        @Transactional
        public void deleteAddress(Long userId, Long addressId) {
            var address = loadOwned(userId, addressId);
            addressRepository.delete(address);
            if (address.isDefaultAddress()) {
                // 다른 주소 중 가장 최근 것을 기본으로 승격
                addressRepository.findAllByUserId(userId).stream()
                    .max(Comparator.comparing(UserAddress::getCreatedAt))
                    .ifPresent(next -> {
                        next.setAsDefault();
                        addressRepository.save(next);
                    });
            }
        }

        @Override
        @Transactional
        public UserAddress setDefault(Long userId, Long addressId) {
            var address = loadOwned(userId, addressId);
            unsetCurrentDefault(userId);
            address.setAsDefault();
            return addressRepository.save(address);
        }

        @Override
        public Optional<UserAddress> findDefault(Long userId) {
            return addressRepository.findAllByUserId(userId).stream()
                .filter(UserAddress::isDefaultAddress)
                .findFirst();
        }

        @Override
        public List<UserAddress> listAll(Long userId) {
            return addressRepository.findAllByUserId(userId).stream()
                .sorted(Comparator.comparing(UserAddress::isDefaultAddress).reversed()
                    .thenComparing(Comparator.comparing(UserAddress::getCreatedAt).reversed()))
                .toList();
        }

        @Override
        public long count(Long userId) {
            return addressRepository.countByUserId(userId);
        }

        @Override
        public void assertOwnership(Long userId, Long addressId) {
            loadOwned(userId, addressId);
        }

        // helpers
        private UserAddress loadOwned(Long userId, Long addressId) {
            var address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("주소를 찾을 수 없습니다: " + addressId));
            if (!address.getUserId().equals(userId)) {
                throw new InvalidStateException("타 사용자의 주소에는 접근할 수 없습니다.");
            }
            return address;
        }

        private void unsetCurrentDefault(Long userId) {
            addressRepository.findAllByUserId(userId).stream()
                .filter(UserAddress::isDefaultAddress)
                .forEach(a -> {
                    a.unsetDefault();
                    addressRepository.save(a);
                });
        }

        private void validate(AddressCommand cmd) {
            if (cmd.recipientName() == null || cmd.recipientName().isBlank())
                throw new InvalidStateException("수령인 이름은 필수입니다.");
            if (cmd.phoneNumber() == null || cmd.phoneNumber().isBlank())
                throw new InvalidStateException("연락처는 필수입니다.");
            if (cmd.postalCode() == null || cmd.postalCode().isBlank())
                throw new InvalidStateException("우편번호는 필수입니다.");
            if (cmd.addressLine1() == null || cmd.addressLine1().isBlank())
                throw new InvalidStateException("주소는 필수입니다.");
        }
    }
}
