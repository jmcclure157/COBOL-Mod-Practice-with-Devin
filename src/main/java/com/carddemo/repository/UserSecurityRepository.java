package com.carddemo.repository;

import com.carddemo.model.UserSecurity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSecurityRepository extends JpaRepository<UserSecurity, String> {
}
