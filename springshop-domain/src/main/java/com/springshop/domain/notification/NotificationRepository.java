package com.springshop.domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 Repository.
 *
 * <p>사용자별 알림 페이징, 미읽음 카운트, 일괄 읽음 처리, 오래된 읽음 알림 삭제 등 운영
 * 배치에서 사용하는 메서드들을 정의한다.</p>
 *
 * @author SpringShop Domain Team
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자 알림 최신순.
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 사용자 미읽음 알림.
     */
    @Query("""
            SELECT n FROM Notification n
             WHERE n.userId = :userId
               AND n.isRead = false
             ORDER BY n.priority DESC, n.createdAt DESC
            """)
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * 사용자 미읽음 알림 페이징.
     */
    Page<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 미읽음 카운트.
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * 사용자의 미읽음 알림 일괄 읽음 처리.
     */
    @Modifying
    @Query("""
            UPDATE Notification n
               SET n.isRead = true,
                   n.readAt = CURRENT_TIMESTAMP
             WHERE n.userId = :userId
               AND n.isRead = false
            """)
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 오래된 읽음 알림 삭제 (배치).
     */
    @Modifying
    @Query("""
            DELETE FROM Notification n
             WHERE n.isRead = true
               AND n.readAt < :cutoffDate
            """)
    int deleteOldRead(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 만료된 알림 삭제.
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /**
     * 사용자별 최근 알림.
     */
    @Query("""
            SELECT n FROM Notification n
             WHERE n.userId = :userId
               AND n.createdAt >= :since
             ORDER BY n.createdAt DESC
            """)
    List<Notification> findRecentByUserId(@Param("userId") Long userId,
                                          @Param("since") LocalDateTime since);

    /**
     * 사용자 + 타입.
     */
    Page<Notification> findByUserIdAndTypeCodeOrderByCreatedAtDesc(Long userId, String typeCode, Pageable pageable);

    /**
     * 우선순위별.
     */
    @Query("""
            SELECT n FROM Notification n
             WHERE n.userId = :userId
               AND n.priority = :priority
             ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdAndPriority(@Param("userId") Long userId,
                                               @Param("priority") Notification.Priority priority);
}
