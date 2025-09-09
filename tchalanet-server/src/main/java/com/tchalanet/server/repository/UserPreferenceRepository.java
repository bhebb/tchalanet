package com.tchalanet.server.repository;

import com.tchalanet.server.model.UserPreference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, UUID> {}
