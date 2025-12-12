package com.teamloci.loci.domain.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n " +
            "WHERE n.receiver.id = :userId " +
            "AND (:cursorId IS NULL OR n.id < :cursorId) " +
            "ORDER BY n.id DESC")
    List<Notification> findByUserIdWithCursor(@Param("userId") Long userId, @Param("cursorId") Long cursorId, Pageable pageable);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.receiver.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);
}