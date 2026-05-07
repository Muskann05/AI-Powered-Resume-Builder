package com.resumeai.export.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.export.dto.ResumeSummaryResponse;
import com.resumeai.export.dto.SectionResponse;
import com.resumeai.export.dto.TemplateResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class ExportContentBuilderService {

    private final ObjectMapper objectMapper;

    public ExportContentBuilderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildResumeJson(ResumeSummaryResponse resume,
                                  List<SectionResponse> sections,
                                  String customizations) {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("resumeId", resume.resumeId());
            root.put("userId", resume.userId());
            root.put("title", resume.title());
            root.put("targetJobTitle", resume.targetJobTitle());
            root.put("templateId", resume.templateId());
            root.put("atsScore", resume.atsScore());
            root.put("status", resume.status());
            root.put("language", resume.language());
            root.put("isPublic", resume.isPublic());
            root.put("viewCount", resume.viewCount());
            root.put("sections", sections);
            root.put("customizations", customizations);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to build resume export JSON", ex);
        }
    }

    public String buildHtml(ResumeSummaryResponse resume,
                            List<SectionResponse> sections,
                            TemplateResponse template) {
        String sectionsHtml = buildAllSectionsHtml(sections);

        String templateHtml = template.htmlLayout();
        if (templateHtml != null && !templateHtml.isBlank()) {
            Map<String, String> placeholders = buildPlaceholderMap(resume, sections, sectionsHtml);
            String rendered = templateHtml;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return rendered.replace("{{sections}}", sectionsHtml);
        }

        return """
                <html><body><div class='resume'><h1>%s</h1><h3>%s</h3>%s</div></body></html>
                """.formatted(safe(resume.title()), safe(resume.targetJobTitle()), sectionsHtml);
    }

    public String buildCss(TemplateResponse template) {
        if (template.cssStyles() != null && !template.cssStyles().isBlank()) {
            return template.cssStyles();
        }
        return """
                body { font-family: Arial, sans-serif; color: #222; margin: 24px; }
                .resume { max-width: 900px; margin: 0 auto; }
                h1 { margin-bottom: 4px; }
                h2 { margin-top: 20px; border-bottom: 1px solid #ddd; padding-bottom: 4px; }
                section { margin-top: 14px; }
                """;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String buildAllSectionsHtml(List<SectionResponse> sections) {
        StringBuilder html = new StringBuilder();
        for (SectionResponse section : sections) {
            if (Boolean.FALSE.equals(section.isVisible())) {
                continue;
            }
            html.append("<section>");
            html.append("<h2>").append(safe(section.title())).append("</h2>");
            html.append("<div>").append(safe(section.content())).append("</div>");
            html.append("</section>");
        }
        return html.toString();
    }

    private Map<String, String> buildPlaceholderMap(ResumeSummaryResponse resume,
                                                    List<SectionResponse> sections,
                                                    String sectionsHtml) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("resumeTitle", safe(resume.title()));
        placeholders.put("targetJobTitle", safe(resume.targetJobTitle()));
        placeholders.put("allSections", sectionsHtml);

        putSectionPlaceholder(placeholders, "personalInfoSection", sections, "PERSONAL");
        putSectionPlaceholder(placeholders, "summarySection", sections, "SUMMARY");
        putSectionPlaceholder(placeholders, "experienceSection", sections, "EXPERIENCE");
        putSectionPlaceholder(placeholders, "educationSection", sections, "EDUCATION");
        putSectionPlaceholder(placeholders, "skillsSection", sections, "SKILLS");
        putSectionPlaceholder(placeholders, "projectsSection", sections, "PROJECTS");

        placeholders.put("fullName", safe(resume.title()));
        placeholders.put("headline", safe(resume.targetJobTitle()));
        placeholders.put("summary", placeholders.get("summarySection"));
        placeholders.put("experience", placeholders.get("experienceSection"));
        placeholders.put("education", placeholders.get("educationSection"));
        placeholders.put("skills", placeholders.get("skillsSection"));
        placeholders.put("projects", placeholders.get("projectsSection"));
        placeholders.put("email", "");
        placeholders.put("phone", "");
        placeholders.put("location", "");
        placeholders.put("linkedin", "");
        return placeholders;
    }

    private void putSectionPlaceholder(Map<String, String> placeholders,
                                       String placeholderName,
                                       List<SectionResponse> sections,
                                       String matchToken) {
        String content = sections.stream()
                .filter(section -> Boolean.TRUE.equals(section.isVisible()))
                .filter(section -> matchesSection(section, matchToken))
                .map(SectionResponse::content)
                .findFirst()
                .orElse("");
        placeholders.put(placeholderName, safe(content));
    }

    private boolean matchesSection(SectionResponse section, String matchToken) {
        String type = safe(section.sectionType()).toUpperCase(Locale.ROOT);
        String title = safe(section.title()).toUpperCase(Locale.ROOT);
        return type.contains(matchToken) || title.contains(matchToken);
    }
}
