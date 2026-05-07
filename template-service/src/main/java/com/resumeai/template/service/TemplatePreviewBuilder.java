package com.resumeai.template.service;

import com.resumeai.template.entity.ResumeTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TemplatePreviewBuilder {

    public String buildPreviewHtml(ResumeTemplate template) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("fullName", "Aarav Sharma");
        placeholders.put("headline", "Java Backend Developer");
        placeholders.put("email", "aarav.sharma@example.com");
        placeholders.put("phone", "+91 98765 43210");
        placeholders.put("location", "Pune, India");
        placeholders.put("linkedin", "linkedin.com/in/aaravsharma");
        placeholders.put("summary", "Backend engineer with strong experience in Spring Boot, REST APIs, MySQL, and production support.");
        placeholders.put("experience", "<article><h3>Backend Developer</h3><p>Built resume, export, and notification microservices with queue-safe fallbacks.</p></article>");
        placeholders.put("education", "<article><h3>B.Tech, Computer Science</h3><p>2020 - 2024</p></article>");
        placeholders.put("skills", "Java, Spring Boot, REST API, MySQL, RabbitMQ, Docker");
        placeholders.put("projects", "<article><h3>ResumeAI</h3><p>AI-powered resume builder with job matching and template-based exports.</p></article>");
        placeholders.put("personalInfoSection", "Aarav Sharma<br/>aarav.sharma@example.com<br/>+91 98765 43210<br/>Pune, India");
        placeholders.put("summarySection", "Backend engineer with strong experience in Spring Boot, REST APIs, MySQL, and production support.");
        placeholders.put("experienceSection", "<article><h3>Backend Developer</h3><p>Built resume, export, and notification microservices with queue-safe fallbacks.</p></article>");
        placeholders.put("educationSection", "<article><h3>B.Tech, Computer Science</h3><p>2020 - 2024</p></article>");
        placeholders.put("skillsSection", "Java, Spring Boot, REST API, MySQL, RabbitMQ, Docker");
        placeholders.put("projectsSection", "<article><h3>ResumeAI</h3><p>AI-powered resume builder with job matching and template-based exports.</p></article>");
        placeholders.put("resumeTitle", "Aarav Sharma Resume");
        placeholders.put("targetJobTitle", "Java Backend Developer");
        placeholders.put("allSections", "<section><h2>Summary</h2><p>Backend engineer with strong experience in Spring Boot, REST APIs, MySQL, and production support.</p></section>");

        String html = template.getHtmlLayout();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }

        return """
                <html>
                  <head>
                    <meta charset="UTF-8"/>
                    <style>%s</style>
                  </head>
                  <body>%s</body>
                </html>
                """.formatted(template.getCssStyles(), html);
    }
}
