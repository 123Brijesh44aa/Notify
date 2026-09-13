package com.brijesh.notify.repository;

import com.brijesh.notify.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    Optional<Notification> findByDedupKey(String dedupKey);
    boolean existsByDedupKey(String dedupKey);
}
