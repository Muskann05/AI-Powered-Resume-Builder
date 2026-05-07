package com.resumeai.jobmatch.client;

import com.resumeai.jobmatch.dto.LinkedInJobResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "linkedin-job-client", url = "${linkedin.api.base-url}")
public interface LinkedInJobClient {

    @GetMapping("${linkedin.api.search-path}")
    List<LinkedInJobResponse> searchJobs(@RequestParam("title") String title,
                                         @RequestParam("location") String location,
                                         @RequestParam("limit") Integer limit,
                                         @RequestHeader("X-Api-Key") String apiKey);
}