package com.ftn.drumigo.repository;

import com.ftn.drumigo.domain.enums.UserRole;
import com.ftn.drumigo.domain.users.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByEmailContainingIgnoreCase(String emailPart, Pageable pageable);
    Page<User> findByRoleIn(Collection<UserRole> roles, Pageable pageable);
    Optional<User> findFirstByRoleOrderByIdAsc(UserRole role);
}

