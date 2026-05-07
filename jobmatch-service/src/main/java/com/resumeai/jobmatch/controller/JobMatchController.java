package com.resumeai.jobmatch.controller;

import com.resumeai.jobmatch.dto.AnalyzeJobFitRequest;
import com.resumeai.jobmatch.dto.ApiMessageResponse;
import com.resumeai.jobmatch.dto.BookmarkMatchRequest;
import com.resumeai.jobmatch.dto.JobMatchResponse;
import com.resumeai.jobmatch.dto.JobSearchRequest;
import com.resumeai.jobmatch.dto.TailoringRecommendationsResponse;
import com.resumeai.jobmatch.service.JobMatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/job-matches")
public class JobMatchController {

    private final JobMatchService jobMatchService;

    public JobMatchController(JobMatchService jobMatchService) {
        this.jobMatchService = jobMatchService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<JobMatchResponse> analyzeJobFit(@Valid @RequestBody AnalyzeJobFitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobMatchService.analyzeJobFit(request));
    }

    @PostMapping("/fetch/linkedin")
    public ResponseEntity<List<JobMatchResponse>> fetchLinkedInJobs(@Valid @RequestBody JobSearchRequest request) {
        return ResponseEntity.ok(jobMatchService.fetchLinkedInJobs(request));
    }

    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<List<JobMatchResponse>> getMatchesByResume(@PathVariable String resumeId) {
        return ResponseEntity.ok(jobMatchService.getMatchesByResume(resumeId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<JobMatchResponse>> getMatchesByUser(@PathVariable String userId) {
        return ResponseEntity.ok(jobMatchService.getMatchesByUser(userId));
    }

    @GetMapping("/user/{userId}/top")
    public ResponseEntity<List<JobMatchResponse>> getTopMatches(@PathVariable String userId) {
        return ResponseEntity.ok(jobMatchService.getTopMatches(userId));
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<JobMatchResponse> getMatchById(@PathVariable String matchId) {
        return ResponseEntity.ok(jobMatchService.getMatchById(matchId));
    }

    @PutMapping("/{matchId}/bookmark")
    public ResponseEntity<JobMatchResponse> bookmarkMatch(@PathVariable String matchId,
                                                          @Valid @RequestBody BookmarkMatchRequest request) {
        return ResponseEntity.ok(jobMatchService.bookmarkMatch(matchId, request.bookmarked()));
    }

    @GetMapping("/{matchId}/recommendations")
    public ResponseEntity<TailoringRecommendationsResponse> getRecommendations(@PathVariable String matchId) {
        return ResponseEntity.ok(jobMatchService.getRecommendations(matchId));
    }

    @DeleteMapping("/{matchId}")
    public ResponseEntity<ApiMessageResponse> deleteMatch(@PathVariable String matchId) {
        jobMatchService.deleteMatch(matchId);
        return ResponseEntity.ok(new ApiMessageResponse("Job match deleted successfully"));
    }
}
