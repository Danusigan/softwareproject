package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModuleRepository extends JpaRepository<Module, String> {
    List<Module> findByIsDeletedFalse();
    List<Module> findByIsDeletedTrue();
    Optional<Module> findByModuleIdAndIsDeletedFalse(String moduleId);
}