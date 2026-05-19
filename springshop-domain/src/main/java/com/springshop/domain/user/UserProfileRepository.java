package com.springshop.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 프로필 Repository.
 *
 * <p>1:1 매핑된 {@link UserProfile}을 사용자 ID 기준으로 조회한다.
 * 뉴스레터/마케팅 대상 추출용 쿼리를 함께 제공한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * 사용자 ID로 프로필 조회.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.user.id = :userId")
    Optional<UserProfile> findByUserId(@Param("userId") Long userId);

    /**
     * 닉네임 중복 여부.
     */
    @Query("SELECT COUNT(p) > 0 FROM UserProfile p WHERE p.nickname = :nickname")
    boolean existsByNickname(@Param("nickname") String nickname);

    /**
     * 닉네임으로 단일 프로필 조회.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.nickname = :nickname")
    Optional<UserProfile> findByNickname(@Param("nickname") String nickname);

    /**
     * 뉴스레터 구독자 모두.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.newsletterSubscribed = true")
    List<UserProfile> findNewsletterSubscribers();

    /**
     * 이메일 마케팅 동의자.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.marketingEmailConsent = true")
    List<UserProfile> findEmailMarketingOptIns();

    /**
     * SMS 마케팅 동의자.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.marketingSmsConsent = true")
    List<UserProfile> findSmsMarketingOptIns();

    /**
     * 특정 언어 사용자.
     */
    @Query("SELECT p FROM UserProfile p WHERE p.preferredLanguage = :lang")
    List<UserProfile> findByPreferredLanguage(@Param("lang") String lang);

    /**
     * 사용자 ID 삭제 시 프로필 cascade 제거가 어려운 환경에서 호출.
     */
    @Query("DELETE FROM UserProfile p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
