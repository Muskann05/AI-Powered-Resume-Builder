package com.resumeai.resume.repository;

import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.enums.ResumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, String> {

    List<Resume> findByUserId(String userId);

    Optional<Resume> findByResumeId(String resumeId);

    List<Resume> findByStatus(ResumeStatus status);

    List<Resume> findByTargetJobTitle(String targetJobTitle);

    List<Resume> findByIsPublicTrue();

    List<Resume> findByIsPublicTrueOrderByViewCountDescCreatedAtDesc();

    List<Resume> findTop12ByIsPublicTrueOrderByViewCountDescCreatedAtDesc();

    List<Resume> findByIsPublicTrueAndTargetJobTitleContainingIgnoreCaseOrderByViewCountDescCreatedAtDesc(String keyword);

    long countByUserId(String userId);

    List<Resume> findByTemplateId(String templateId);

    void deleteByResumeId(String resumeId);
}
