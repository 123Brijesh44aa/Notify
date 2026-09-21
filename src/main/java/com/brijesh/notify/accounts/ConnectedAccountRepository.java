package com.brijesh.notify.accounts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectedAccountRepository extends JpaRepository<ConnectedAccount,Long> {

    Optional<ConnectedAccount> findByUserIdAndProvider(Long userId, String provider);
    List<ConnectedAccount> findByUserId(Long userId);

    Optional<ConnectedAccount> findByProviderAndExternalUsernameIgnoreCase(String provider, String externalUsername);
}
