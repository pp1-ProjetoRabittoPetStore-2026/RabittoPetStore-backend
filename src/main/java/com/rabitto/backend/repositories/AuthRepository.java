package com.rabitto.backend.repositories;

import com.rabitto.backend.models.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findByRefreshTokenAndRevokedFalse(String refreshToken);

    void deleteByTutorId(Long tutorId);

}
