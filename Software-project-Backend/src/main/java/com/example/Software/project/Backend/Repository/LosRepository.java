package com.example.Software.project.Backend.Repository;

import com.example.Software.project.Backend.Model.Los;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LosRepository extends JpaRepository<Los, String> {

    // Find all Los belonging to a specific module (by Module ID)
    List<Los> findByModule_ModuleId(String moduleId);
}
