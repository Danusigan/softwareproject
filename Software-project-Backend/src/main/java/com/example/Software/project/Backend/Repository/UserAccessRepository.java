package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.UserAccess;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccessRepository extends JpaRepository<UserAccess, String> {
}