package com.speakup.repository;

import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

    List<Prompt> findByModeAndActiveTrue(Mode mode);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.category.name = :categoryName")
    List<Prompt> findByModeAndCategoryName(@Param("mode") Mode mode, @Param("categoryName") String categoryName);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.id <> :excludeId")
    List<Prompt> findByModeExcluding(@Param("mode") Mode mode, @Param("excludeId") Long excludeId);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.category.name = :categoryName AND p.id <> :excludeId")
    List<Prompt> findByModeAndCategoryNameExcluding(@Param("mode") Mode mode, @Param("categoryName") String categoryName, @Param("excludeId") Long excludeId);
}
