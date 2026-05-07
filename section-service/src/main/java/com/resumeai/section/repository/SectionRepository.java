package com.resumeai.section.repository;

import com.resumeai.section.entity.ResumeSection;
import com.resumeai.section.enums.SectionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<ResumeSection, String> {

    List<ResumeSection> findByResumeId(String resumeId);

    List<ResumeSection> findByResumeIdAndSectionType(String resumeId, SectionType sectionType);

    Optional<ResumeSection> findBySectionId(String sectionId);

    List<ResumeSection> findByResumeIdOrderByDisplayOrderAsc(String resumeId);

    List<ResumeSection> findByAiGenerated(Boolean aiGenerated);

    long countByResumeId(String resumeId);

    void deleteByResumeId(String resumeId);

    void deleteBySectionId(String sectionId);
}
