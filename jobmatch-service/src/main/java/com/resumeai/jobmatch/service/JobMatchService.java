package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.JobMatchResponse;
import com.resumeai.jobmatch.dto.JobSearchRequest;
import com.resumeai.jobmatch.dto.TailoringRecommendationsResponse;

import java.util.List;

public interface JobMatchService {

    JobMatchResponse analyzeJobFit(AnalyzeJobFitRequest request);

    List<JobMatchResponse> fetchLinkedInJobs(JobSearchRequest request);

    List<JobMatchResponse> getMatchesByResume(String resumeId);

    List<JobMatchResponse> getMatchesByUser(String userId);

    List<JobMatchResponse> getTopMatches(String userId);

    JobMatchResponse getMatchById(String matchId);

    JobMatchResponse bookmarkMatch(String matchId, Boolean bookmarked);

    TailoringRecommendationsResponse getRecommendations(String matchId);

    void deleteMatch(String matchId);
}
