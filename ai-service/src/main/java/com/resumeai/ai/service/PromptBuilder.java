package com.resumeai.ai.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\u0000-\\u001F]", " ").trim();
    }

    public String summaryPrompt(String jobTitle, String yearsOfExperience, String keySkills) {
        return """
                You are an expert resume-writing assistant.
                Generate a professional ATS-friendly resume summary in 4-6 lines.

                Job Title: %s
                Experience: %s
                Key Skills: %s

                Return plain text only.
                """.formatted(sanitize(jobTitle), sanitize(yearsOfExperience), sanitize(keySkills));
    }

    public String bulletPrompt(String jobRole, String companyName, String responsibilities) {
        return """
                Write 5 strong ATS-friendly resume bullet points.

                Job Role: %s
                Company: %s
                Responsibilities: %s

                Return only bullet points in plain text.
                """.formatted(sanitize(jobRole), sanitize(companyName), sanitize(responsibilities));
    }

    public String coverLetterPrompt(String applicantName, String jobDescription) {
        return """
                Write a personalized professional cover letter.

                Applicant: %s
                Job Description: %s

                Return only final cover letter text.
                """.formatted(sanitize(applicantName), sanitize(jobDescription));
    }

    public String improveSectionPrompt(String sectionName, String currentContent) {
        return """
                Improve this resume section for impact, clarity, professionalism and ATS relevance.

                Section Name: %s
                Current Content: %s

                Return only improved section text.
                """.formatted(sanitize(sectionName), sanitize(currentContent));
    }

    public String skillsPrompt(String targetJobTitle) {
        return """
                Suggest 12-15 relevant ATS-friendly resume skills for this role.

                Target Job Title: %s

                Return comma-separated skills only.
                """.formatted(sanitize(targetJobTitle));
    }

    public String tailorPrompt(String resumeJson, String jobDescription) {
        return """
                Tailor the following resume JSON for the job description.

                Resume JSON: %s
                Job Description: %s

                Return valid updated JSON only.
                """.formatted(sanitize(resumeJson), sanitize(jobDescription));
    }

    public String translatePrompt(String resumeText, String targetLanguage) {
        return """
                Translate the following resume into %s while preserving professional tone.

                Resume Text: %s

                Return only translated text.
                """.formatted(sanitize(targetLanguage), sanitize(resumeText));
    }

    public String atsRecommendationPrompt(String resumeText, String jobDescription, int score, String missingKeywords) {
        return """
                Analyze ATS improvement opportunities.

                Resume Text: %s
                Job Description: %s
                Current ATS Score: %d
                Missing Keywords: %s

                Return 3-5 concise recommendations in plain text.
                """.formatted(sanitize(resumeText), sanitize(jobDescription), score, sanitize(missingKeywords));
    }

    public String jobFitPrompt(String resumeContent, String jobTitle, String jobDescription) {
        return """
                Analyze the resume against the target job.

                Resume Content: %s
                Job Title: %s
                Job Description: %s

                Return:
                1. Match score from 0 to 100
                2. Missing skills as comma-separated values
                3. 3 short tailoring recommendations
                """.formatted(sanitize(resumeContent), sanitize(jobTitle), sanitize(jobDescription));
    }
}
