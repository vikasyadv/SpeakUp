package com.speakup.repository;

import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

    List<Prompt> findByModeAndActiveTrue(Mode mode);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.category.name = :categoryName")
    List<Prompt> findByModeAndCategoryName(@Param("mode") Mode mode, @Param("categoryName") String categoryName);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.id <> :excludeId")
    List<Prompt> findByModeExcluding(@Param("mode") Mode mode, @Param("excludeId") Long excludeId);

    @Query("SELECT p FROM Prompt p WHERE p.mode = :mode AND p.active = true AND p.category.name = :categoryName AND p.id <> :excludeId")
    List<Prompt> findByModeAndCategoryNameExcluding(@Param("mode") Mode mode, @Param("categoryName") String categoryName, @Param("excludeId") Long excludeId);

    @Query("SELECT DISTINCT p.category FROM Prompt p WHERE p.mode = :mode AND p.active = true ORDER BY p.category.name ASC")
    List<Category> findDistinctCategoriesByMode(@Param("mode") Mode mode);

    boolean existsByModeAndText(Mode mode, String text);

    @Query("SELECT p.text FROM Prompt p WHERE p.mode = :mode")
    Set<String> findTextsByMode(@Param("mode") Mode mode);
}
