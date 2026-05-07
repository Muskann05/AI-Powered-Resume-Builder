package com.resumeai.ai.service;

import org.springframework.stereotype.Component;

@Component
public class FallbackAiGenerator {

    public String generateFallback(String prompt) {
        String lower = prompt.toLowerCase();

        if (lower.contains("resume summary") || lower.contains("professional ats-friendly resume summary")) {
            return """
                    Java backend developer with strong experience in Spring Boot, REST APIs, and MySQL.
                    Skilled in building scalable backend systems and microservices-based applications.
                    Known for writing clean code, optimizing performance, and solving real-world business problems.
                    """;
        }

        if (lower.contains("bullet points")) {
            return """
                    - Developed scalable REST APIs using Java and Spring Boot.
                    - Optimized SQL queries and improved backend performance.
                    - Collaborated with cross-functional teams to deliver production features.
                    - Worked on microservices architecture and service integration.
                    - Maintained clean, testable, and reusable backend code.
                    """;
        }

        if (lower.contains("cover letter")) {
            return """
                    Dear Hiring Manager,

                    I am excited to apply for this opportunity. With experience in Java, Spring Boot, REST APIs, and backend development, I believe I can contribute effectively to your team. My background in building scalable services and solving technical problems aligns well with the requirements of this role.

                    Sincerely,
                    Candidate
                    """;
        }

        if (lower.contains("improve this resume section")) {
            return "Results-driven backend developer with hands-on experience in Java, Spring Boot, REST APIs, and database optimization, known for building scalable and maintainable systems.";
        }

        if (lower.contains("suggest 12-15 relevant ats-friendly resume skills")) {
            return "Java, Spring Boot, REST APIs, MySQL, Microservices, Hibernate, JPA, SQL, Maven, Git, Docker, Problem Solving, Backend Development, API Integration, System Design";
        }

        if (lower.contains("tailor the following resume json")) {
            return """
                    {
                      "summary": "Tailored backend developer profile aligned with Java and Spring Boot job requirements",
                      "skills": ["Java", "Spring Boot", "REST APIs", "Microservices", "MySQL", "Docker"]
                    }
                    """;
        }

        if (lower.contains("translate the following resume")) {
            return "Profil developpeur backend Java experimente avec expertise en Spring Boot et API REST.";
        }

        if (lower.contains("analyze ats improvement opportunities")) {
            return """
                    1. Add more job-specific keywords from the description.
                    2. Highlight tools and technologies that match the target role.
                    3. Rewrite summary and experience using ATS-friendly terminology.
                    4. Include measurable achievements where possible.
                    """;
        }

        return "Fallback AI response generated successfully.";
    }
}
