package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.LosPos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LosPosRepository extends JpaRepository<LosPos, String> {
    List<LosPos> findByModule_ModuleId(String moduleId);
}