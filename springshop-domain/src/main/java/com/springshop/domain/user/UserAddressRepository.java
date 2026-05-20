package com.springshop.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 배송지 Repository.
 *
 * <p>사용자 ID 기준 배송지 조회, 기본 배송지 단건 조회 및 일괄 갱신 쿼리를 제공한다.
 * 기본 배송지는 사용자당 하나뿐이어야 하므로 변경 시 기존 기본 배송지를 해제하는
 * 일괄 UPDATE 쿼리를 함께 노출한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    /**
     * 사용자의 활성 배송지 목록을 정렬 순서대로 조회.
     */
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId AND a.isActive = true ORDER BY a.isDefault DESC, a.id ASC")
    List<UserAddress> findByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 기본 배송지 조회.
     */
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId AND a.isDefault = true AND a.isActive = true")
    Optional<UserAddress> findDefaultByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 배송지 개수.
     */
    @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.user.id = :userId AND a.isActive = true")
    long countActiveByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 모든 기본 배송지 플래그를 false로 설정한다.
     * 새 기본 배송지를 지정하기 전 호출한다.
     */
    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
    int unsetAllDefaults(@Param("userId") Long userId);

    default java.util.List<UserAddress> findAllByUserId(Long userId) { return findByUserId(userId); }
    default long countByUserId(Long userId) { return countActiveByUserId(userId); }

    /**
     * 배송지명으로 검색.
     */
    @Query("SELECT a FROM UserAddress a WHERE a.user.id = :userId AND a.addressName LIKE %:name%")
    List<UserAddress> searchByNameLike(@Param("userId") Long userId, @Param("name") String name);

    /**
     * 특정 우편번호로 등록된 배송지 수(통계용).
     */
    @Query("SELECT COUNT(a) FROM UserAddress a WHERE a.address.zipCode = :zip")
    long countByZipCode(@Param("zip") String zipCode);
}
