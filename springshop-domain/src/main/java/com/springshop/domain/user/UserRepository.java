package com.springshop.domain.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 엔티티 Repository.
 *
 * <p>Spring Data JPA의 표준 메서드와 함께 도메인 특화 쿼리를 제공한다.
 * 모든 메서드는 트랜잭션 컨텍스트 안에서 호출되어야 한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일 값으로 사용자 조회. 임베디드 Email.value 컬럼 기준.
     */
    @Query("SELECT u FROM User u WHERE u.email.value = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /**
     * 이메일 존재 여부.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email.value = :email")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 특정 상태 라벨의 사용자 목록.
     */
    @Query("SELECT u FROM User u WHERE u.statusLabel = :status")
    List<User> findByStatus(@Param("status") String statusLabel);

    /**
     * 활성 사용자만 페이징 조회.
     */
    @Query("SELECT u FROM User u WHERE u.statusLabel = 'ACTIVE' AND u.deleted = false")
    Page<User> findActiveUsers(Pageable pageable);

    /**
     * 역할별 사용자 수.
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.statusLabel = 'ACTIVE'")
    long countActiveByRole(@Param("role") UserRole role);

    /**
     * 마지막 로그인이 일정 시점 이전인 휴면 후보 사용자.
     */
    @Query("SELECT u FROM User u WHERE u.statusLabel = 'ACTIVE' AND (u.lastLoginAt IS NULL OR u.lastLoginAt < :threshold)")
    List<User> findDormantCandidates(@Param("threshold") LocalDateTime threshold);

    /**
     * 자동 잠금 해제 시간이 지난 잠금 사용자.
     */
    @Query("SELECT u FROM User u WHERE u.statusLabel = 'LOCKED' AND u.statusUnlockAt IS NOT NULL AND u.statusUnlockAt <= :now")
    List<User> findUsersToAutoUnlock(@Param("now") LocalDateTime now);

    /**
     * 마케팅 동의자 조회.
     */
    @Query("SELECT u FROM User u WHERE u.marketingConsent = true AND u.statusLabel = 'ACTIVE'")
    List<User> findMarketingOptIns();

    /**
     * 이름 부분 일치 검색(관리자용).
     */
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    Page<User> searchByName(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 닉네임(이름) 존재 여부 확인. User 엔티티에 별도 nickname 필드가 없어 name으로 대체.
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.name = :nickname")
    boolean existsByNickname(@Param("nickname") String nickname);
}
