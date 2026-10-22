
package com.mayank.cabbooking.repository;

import com.mayank.cabbooking.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);


    Optional<Auth> findByEmailIgnoreCase(String email);


}
