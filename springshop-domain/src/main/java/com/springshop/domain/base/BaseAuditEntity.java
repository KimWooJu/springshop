package com.springshop.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Objects;
import java.util.Optional;

/**
 * 감사 정보(생성자/수정자)가 필요한 엔티티의 기본 클래스.
 *
 * <p>{@link BaseEntity}가 제공하는 id/createdAt/updatedAt/version 위에
 * createdBy, updatedBy 필드를 추가한다. Spring Data JPA AuditorAware
 * Bean을 통해 현재 인증된 사용자의 식별자(보통 사용자명 또는 ID)가 자동으로 채워진다.</p>
 *
 * <p>주문, 결제, 환불 등 누가 생성·수정했는지 추적이 필수인 도메인 엔티티에서 사용한다.</p>
 *
 * @author SpringShop Domain Team
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity extends BaseEntity {

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    protected BaseAuditEntity() {
        super();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    /**
     * 소프트 삭제 처리. 실제 행은 보존된다.
     *
     * @param performedBy 삭제 처리자
     * @param reason 삭제 사유
     */
    public void markDeleted(String performedBy, String reason) {
        Objects.requireNonNull(performedBy, "삭제자가 비어있습니다");
        if (this.deleted) {
            throw new IllegalStateException("이미 삭제된 엔티티입니다: " + getId());
        }
        this.deleted = true;
        this.deletedBy = performedBy;
        this.deletionReason = Optional.ofNullable(reason).orElse("사유 없음");
    }

    /**
     * 소프트 삭제를 되돌린다.
     */
    public void restore() {
        if (!this.deleted) {
            throw new IllegalStateException("삭제되지 않은 엔티티입니다: " + getId());
        }
        this.deleted = false;
        this.deletedBy = null;
        this.deletionReason = null;
    }

    /**
     * 같은 사용자가 생성하고 수정한 엔티티인지 검사한다.
     */
    public boolean isSameAuthor() {
        return Objects.equals(createdBy, updatedBy);
    }

    /**
     * 감사 정보 요약 문자열을 반환한다.
     */
    public String auditSummary() {
        return """
                AuditInfo {
                    createdBy = %s,
                    updatedBy = %s,
                    deleted   = %s,
                    deletedBy = %s
                }""".formatted(createdBy, updatedBy, deleted, deletedBy);
    }
}
