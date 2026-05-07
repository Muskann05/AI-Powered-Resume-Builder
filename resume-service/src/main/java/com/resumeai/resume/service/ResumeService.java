package com.resumeai.resume.service;

import com.resumeai.resume.dto.CreateResumeRequest;
import com.resumeai.resume.dto.ResumeAdminStatsResponse;
import com.resumeai.resume.dto.ResumeResponse;
import com.resumeai.resume.dto.UpdateResumeRequest;

import java.util.List;

public interface ResumeService {

    ResumeResponse createResume(CreateResumeRequest request);

    ResumeResponse getResumeById(String resumeId);

    List<ResumeResponse> getResumesByUser(String userId);

    ResumeResponse updateResume(String resumeId, UpdateResumeRequest request);

    void deleteResume(String resumeId);

    ResumeResponse duplicateResume(String resumeId);

    ResumeResponse updateAtsScore(String resumeId, Integer atsScore);

    ResumeResponse publishResume(String resumeId);

    ResumeResponse unpublishResume(String resumeId);

    ResumeResponse getPublicResume(String resumeId);

    List<ResumeResponse> getPublicResumes();

    List<ResumeResponse> searchPublicResumes(String keyword);

    ResumeResponse incrementViewCount(String resumeId);

    List<ResumeResponse> getResumesByTemplate(String templateId);

    List<ResumeResponse> getAllResumesForAdmin();

    ResumeAdminStatsResponse getAdminStats();
}
