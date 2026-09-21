package com.brijesh.notify.accounts;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "connected_accounts",uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","provider"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "external_username")
    private String externalUsername;

    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;

    private String scope;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @PrePersist
    public void prePersist() {
        this.connectedAt = LocalDateTime.now();
    }
}
