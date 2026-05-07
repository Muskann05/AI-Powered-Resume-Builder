package com.resumeai.template.service;

import com.resumeai.template.entity.ResumeTemplate;
import com.resumeai.template.enums.TemplateCategory;
import com.resumeai.template.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateSeedService {

    private static final Logger log = LoggerFactory.getLogger(TemplateSeedService.class);

    @Bean
    ApplicationRunner seedTemplates(TemplateRepository templateRepository,
                                    @Value("${template.seed.enabled:true}") boolean seedEnabled,
                                    @Value("${template.seed.force-update:false}") boolean forceUpdate) {
        return args -> {
            if (!seedEnabled) {
                log.info("Template seed disabled by configuration");
                return;
            }

            upsertTemplate(templateRepository, forceUpdate,
                    "Professional Classic",
                    "Balanced ATS-friendly layout for most roles.",
                    "/templates/thumbnails/professional-classic.svg",
                    TemplateCategory.PROFESSIONAL,
                    false,
                    """
                    <div class="resume-shell professional-classic">
                      <header class="hero">
                        <div>
                          <p class="eyebrow">Professional Resume</p>
                          <h1>{{resumeTitle}}</h1>
                          <p class="headline">{{targetJobTitle}}</p>
                        </div>
                        <div class="contact-card">
                          <div>{{personalInfoSection}}</div>
                        </div>
                      </header>
                      <section>
                        <h2>Summary</h2>
                        <div>{{summarySection}}</div>
                      </section>
                      <section>
                        <h2>Experience</h2>
                        <div class="timeline-item">{{experienceSection}}</div>
                      </section>
                      <section class="two-column">
                        <div>
                          <h2>Education</h2>
                          <div>{{educationSection}}</div>
                        </div>
                        <div>
                          <h2>Skills</h2>
                          <div class="chip-list">{{skillsSection}}</div>
                        </div>
                      </section>
                    </div>
                    """,
                    """
                    body { font-family: 'Segoe UI', sans-serif; background: #eef2f7; color: #14213d; }
                    .resume-shell { max-width: 900px; margin: 0 auto; background: #ffffff; padding: 40px; }
                    .hero { display: flex; justify-content: space-between; gap: 24px; border-bottom: 3px solid #1d3557; padding-bottom: 20px; }
                    .eyebrow { text-transform: uppercase; letter-spacing: 0.2em; color: #457b9d; font-size: 12px; }
                    .headline { color: #5c677d; }
                    .contact-card { text-align: right; font-size: 14px; }
                    h1, h2 { margin: 0; }
                    h2 { margin-top: 24px; margin-bottom: 12px; color: #1d3557; }
                    .two-column { display: grid; grid-template-columns: 1fr 1fr; gap: 24px; }
                    .chip-list { display: flex; flex-wrap: wrap; gap: 8px; }
                    """
            );

            upsertTemplate(templateRepository, forceUpdate,
                    "Creative Spotlight",
                    "Portfolio-style layout for design, product, and content roles.",
                    "/templates/thumbnails/creative-spotlight.svg",
                    TemplateCategory.CREATIVE,
                    true,
                    """
                    <div class="resume-shell creative-spotlight">
                      <aside class="side-panel">
                        <h1>{{resumeTitle}}</h1>
                        <p class="headline">{{targetJobTitle}}</p>
                        <div class="stack">
                          <h3>Contact</h3>
                          <div>{{personalInfoSection}}</div>
                        </div>
                        <div class="stack">
                          <h3>Skills</h3>
                          <div>{{skillsSection}}</div>
                        </div>
                      </aside>
                      <main class="content-panel">
                        <section>
                          <h2>Profile</h2>
                          <div>{{summarySection}}</div>
                        </section>
                        <section>
                          <h2>Selected Experience</h2>
                          <div>{{experienceSection}}</div>
                        </section>
                        <section>
                          <h2>Projects</h2>
                          <div>{{projectsSection}}</div>
                        </section>
                      </main>
                    </div>
                    """,
                    """
                    body { font-family: Georgia, serif; background: linear-gradient(135deg, #fdf0d5, #f8edeb); color: #3d405b; }
                    .resume-shell { max-width: 960px; margin: 0 auto; display: grid; grid-template-columns: 280px 1fr; min-height: 100vh; }
                    .side-panel { background: #3d405b; color: #fdf0d5; padding: 36px 28px; }
                    .content-panel { background: #ffffff; padding: 36px; }
                    .headline { color: #f2cc8f; }
                    .stack { margin-top: 24px; }
                    h2 { color: #9c6644; border-bottom: 2px solid #f2cc8f; padding-bottom: 6px; }
                    """
            );

            upsertTemplate(templateRepository, forceUpdate,
                    "ATS Precision",
                    "Minimal single-column structure optimised for keyword parsing.",
                    "/templates/thumbnails/ats-precision.svg",
                    TemplateCategory.ATS_OPTIMISED,
                    false,
                    """
                    <div class="resume-shell ats-precision">
                      <header>
                        <h1>{{resumeTitle}}</h1>
                        <p>{{targetJobTitle}}</p>
                        <div>{{personalInfoSection}}</div>
                      </header>
                      <section>
                        <h2>Professional Summary</h2>
                        <div>{{summarySection}}</div>
                      </section>
                      <section>
                        <h2>Core Skills</h2>
                        <div>{{skillsSection}}</div>
                      </section>
                      <section>
                        <h2>Work Experience</h2>
                        <div>{{experienceSection}}</div>
                      </section>
                      <section>
                        <h2>Education</h2>
                        <div>{{educationSection}}</div>
                      </section>
                    </div>
                    """,
                    """
                    body { font-family: Arial, sans-serif; background: #ffffff; color: #111827; }
                    .resume-shell { max-width: 820px; margin: 0 auto; padding: 32px; }
                    header { border-bottom: 1px solid #d1d5db; padding-bottom: 12px; }
                    h1 { font-size: 28px; margin-bottom: 6px; }
                    h2 { font-size: 16px; text-transform: uppercase; letter-spacing: 0.08em; margin-top: 20px; margin-bottom: 10px; }
                    section { margin-bottom: 12px; }
                    """
            );
        };
    }

    private void upsertTemplate(TemplateRepository templateRepository,
                                boolean forceUpdate,
                                String name,
                                String description,
                                String thumbnailUrl,
                                TemplateCategory category,
                                boolean premium,
                                String htmlLayout,
                                String cssStyles) {
        ResumeTemplate template = templateRepository.findByNameIgnoreCase(name).orElseGet(ResumeTemplate::new);

        if (template.getTemplateId() != null && !forceUpdate) {
            log.info("Seed template already exists, skipping name={}", name);
            return;
        }

        template.setName(name);
        template.setDescription(description);
        template.setThumbnailUrl(thumbnailUrl);
        template.setCategory(category);
        template.setIsPremium(premium);
        template.setIsActive(true);
        template.setHtmlLayout(htmlLayout);
        template.setCssStyles(cssStyles);
        if (template.getUsageCount() == null) {
            template.setUsageCount(0);
        }

        templateRepository.save(template);
        log.info("Seeded template name={} premium={} category={}", name, premium, category);
    }
}
