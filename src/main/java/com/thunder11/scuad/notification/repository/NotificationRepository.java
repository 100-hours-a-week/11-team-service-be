package com.thunder11.scuad.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thunder11.scuad.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
            "WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}