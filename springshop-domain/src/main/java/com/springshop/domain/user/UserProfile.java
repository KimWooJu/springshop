package com.springshop.domain.user;

import com.springshop.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * 사용자 프로필 엔티티.
 *
 * <p>{@link User}와 1:1 매핑되며, 닉네임/프로필 이미지/소개/생년월일/성별/마케팅
 * 동의 등 부가 정보를 보관한다. User 본 테이블과 분리하여 자주 변하지 않는
 * 핵심 인증 정보와 자주 갱신되는 프로필 정보를 분리한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Entity
@Table(
        name = "user_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_profile_user", columnNames = "user_id")
)
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "marketing_email_consent", nullable = false)
    private boolean marketingEmailConsent = false;

    @Column(name = "marketing_sms_consent", nullable = false)
    private boolean marketingSmsConsent = false;

    @Column(name = "marketing_push_consent", nullable = false)
    private boolean marketingPushConsent = false;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "ko";

    @Column(name = "newsletter_subscribed", nullable = false)
    private boolean newsletterSubscribed = false;

    protected UserProfile() {
        super();
    }

    public UserProfile(User user, String nickname) {
        super();
        this.user = Objects.requireNonNull(user, "사용자는 필수입니다");
        this.nickname = nickname;
    }

    public static UserProfile forUser(User user) {
        return new UserProfile(user, null);
    }

    public User getUser() {
        return user;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getBio() {
        return bio;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public boolean isMarketingEmailConsent() {
        return marketingEmailConsent;
    }

    public boolean isMarketingSmsConsent() {
        return marketingSmsConsent;
    }

    public boolean isMarketingPushConsent() {
        return marketingPushConsent;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public boolean isNewsletterSubscribed() {
        return newsletterSubscribed;
    }

    public void updateBasicInfo(String nickname, String bio) {
        if (nickname != null && (nickname.length() < 2 || nickname.length() > 30)) {
            throw new IllegalArgumentException("닉네임은 2~30자 사이여야 합니다");
        }
        this.nickname = nickname;
        this.bio = bio;
    }

    public void updateProfileImage(String imageUrl) {
        if (imageUrl != null && imageUrl.length() > 500) {
            throw new IllegalArgumentException("이미지 URL이 너무 깁니다");
        }
        this.profileImageUrl = imageUrl;
    }

    public void updateBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("미래 날짜는 생년월일이 될 수 없습니다");
        }
        this.birthDate = birthDate;
    }

    public void updateGender(String gender) {
        if (gender != null && !gender.matches("MALE|FEMALE|OTHER|UNDISCLOSED")) {
            throw new IllegalArgumentException("성별 값이 유효하지 않습니다: " + gender);
        }
        this.gender = gender;
    }

    public void agreeToAllMarketing() {
        this.marketingEmailConsent = true;
        this.marketingSmsConsent = true;
        this.marketingPushConsent = true;
    }

    public void revokeAllMarketing() {
        this.marketingEmailConsent = false;
        this.marketingSmsConsent = false;
        this.marketingPushConsent = false;
    }

    public void updateMarketingConsents(boolean email, boolean sms, boolean push) {
        this.marketingEmailConsent = email;
        this.marketingSmsConsent = sms;
        this.marketingPushConsent = push;
    }

    public void subscribeNewsletter() {
        this.newsletterSubscribed = true;
    }

    public void unsubscribeNewsletter() {
        this.newsletterSubscribed = false;
    }

    public void changeLanguage(String langCode) {
        if (langCode == null || langCode.isBlank()) {
            throw new IllegalArgumentException("언어 코드가 비어있습니다");
        }
        this.preferredLanguage = langCode;
    }

    /**
     * 만 나이 계산.
     */
    public int currentAge() {
        if (birthDate == null) return -1;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * 미성년자 여부.
     */
    public boolean isMinor() {
        int age = currentAge();
        return age >= 0 && age < 19;
    }
}
