package com.resumeai.jobmatch.service;

import com.resumeai.jobmatch.dto.LinkedInJobResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class LinkedInJobFallbackService {

    public List<LinkedInJobResponse> generateJobs(String title, String location, Integer requestedLimit) {
        int limit = requestedLimit == null || requestedLimit < 1 ? 5 : Math.min(requestedLimit, 10);
        String normalizedTitle = safeValue(title, "Software Engineer");
        String normalizedLocation = safeValue(location, "Remote");
        String slug = (normalizedTitle + "-" + normalizedLocation)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        List<LinkedInJobResponse> jobs = new ArrayList<>();
        String[] companies = {"TechNova", "HireFlow", "SkillBridge", "NextLayer", "BrightPath", "TalentForge"};
        String[] descriptors = {"Growth", "Platform", "Core Product", "Customer Success", "Innovation", "Applied AI"};

        for (int i = 0; i < limit; i++) {
            String company = companies[i % companies.length];
            String descriptor = descriptors[i % descriptors.length];
            jobs.add(new LinkedInJobResponse(
                    slug + "-" + (i + 1),
                    normalizedTitle + " - " + descriptor,
                    company,
                    normalizedLocation,
                    buildDescription(normalizedTitle, normalizedLocation, company, descriptor),
                    "https://www.linkedin.com/jobs/view/" + slug + "-" + (i + 1)
            ));
        }

        return jobs;
    }

    private String buildDescription(String title, String location, String company, String descriptor) {
        return "Dummy LinkedIn listing for " + title + " at " + company + " in " + location + ". "
                + "This fallback job is generated locally when the configured LinkedIn provider is unavailable. "
                + "Focus areas include ATS-friendly resume tailoring, quantified achievements, stakeholder communication, "
                + "and hands-on delivery within the " + descriptor + " team.";
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
