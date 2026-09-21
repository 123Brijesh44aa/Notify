package com.brijesh.notify.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchedRepositoryRepository extends JpaRepository<WatchedRepository, Long> {

    List<WatchedRepository> findByUserId(Long userId);
    Optional<WatchedRepository> findByUserIdAndRepoFullName(Long userId, String repoFullName);
}
