package com.resumeai.template.repository;

import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.enums.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<ResumeTemplate, String> {

    Optional<ResumeTemplate> findByTemplateId(String templateId);

    Optional<ResumeTemplate> findByTemplateIdAndIsActiveTrue(String templateId);

    Optional<ResumeTemplate> findByNameIgnoreCase(String name);

    List<ResumeTemplate> findByCategory(TemplateCategory category);

    List<ResumeTemplate> findByCategoryAndIsActiveTrue(TemplateCategory category);

    List<ResumeTemplate> findByIsPremium(Boolean isPremium);

    List<ResumeTemplate> findByIsPremiumAndIsActiveTrue(Boolean isPremium);

    List<ResumeTemplate> findByIsActive(Boolean isActive);

    List<ResumeTemplate> findAllByOrderByUsageCountDesc();

    List<ResumeTemplate> findAllByIsActiveTrueOrderByUsageCountDesc();

    long countByCategory(TemplateCategory category);
}
