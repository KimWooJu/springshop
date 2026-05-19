package com.springshop.domain.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 모든 JPA 엔티티의 최상위 기본 클래스.
 *
 * <p>식별자(id), 생성 시각(createdAt), 수정 시각(updatedAt), 낙관적 락 버전(version) 을
 * 공통 필드로 제공한다. Spring Data JPA Auditing 기능을 활용하여 자동으로 타임스탬프를
 * 채워준다. 또한 도메인 이벤트(transient)를 등록/소비하기 위한 헬퍼 메서드를 제공한다.</p>
 *
 * <p>도메인 이벤트는 영속화되지 않으며, 트랜잭션 커밋 직전에 ApplicationEventPublisher
 * 로 전달되어 후속 작업(알림, 외부 시스템 연동 등)을 트리거한다.</p>
 *
 * @author SpringShop Domain Team
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 영속화되지 않는 도메인 이벤트 임시 저장소.
     * JPA가 영속 컨텍스트에서 다루지 않도록 transient 처리한다.
     */
    private transient final List<DomainEvent> domainEvents = new ArrayList<>();

    protected BaseEntity() {
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    /**
     * 새로운 도메인 이벤트를 등록한다.
     *
     * @param event 발행할 이벤트 (null 불가)
     */
    public void registerEvent(DomainEvent event) {
        Objects.requireNonNull(event, "이벤트는 null일 수 없습니다");
        this.domainEvents.add(event);
    }

    /**
     * 발행 대기 중인 도메인 이벤트 목록을 반환한다.
     * 불변 뷰를 반환하므로 외부에서 수정 불가능하다.
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * 도메인 이벤트를 모두 비운다. 일반적으로 트랜잭션 커밋 후 호출된다.
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * 영속화 이전 상태인지 검사한다.
     */
    public boolean isNew() {
        return this.id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseEntity other)) return false;
        if (this.id == null || other.id == null) return false;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getSimpleName(), id);
    }

    @Override
    public String toString() {
        return "%s[id=%s, version=%s]".formatted(getClass().getSimpleName(), id, version);
    }
}
