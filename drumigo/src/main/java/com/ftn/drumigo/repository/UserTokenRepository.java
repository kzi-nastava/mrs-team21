package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.UserToken;
import com.ftn.drumigo.domain.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
    Optional<UserToken> findByTokenHashAndTypeAndUsedAtIsNullAndExpiresAtAfter(String tokenHash, TokenType type, Instant now);
}

