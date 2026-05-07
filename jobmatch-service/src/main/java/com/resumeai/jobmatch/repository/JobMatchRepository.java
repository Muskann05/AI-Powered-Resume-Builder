package com.resumeai.jobmatch.repository;

import com.resumeai.jobmatch.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobMatchRepository extends JpaRepository<JobMatch, String> {

    Optional<JobMatch> findByMatchId(String matchId);

    List<JobMatch> findByResumeIdOrderByMatchedAtDesc(String resumeId);

    List<JobMatch> findByUserIdOrderByMatchedAtDesc(String userId);

    List<JobMatch> findTop10ByUserIdOrderByMatchScoreDescMatchedAtDesc(String userId);
}