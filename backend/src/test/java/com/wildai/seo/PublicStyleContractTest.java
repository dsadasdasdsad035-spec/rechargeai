package com.wildai.seo;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PublicStyleContractTest {

    @Test
    void publicStyleReusesUserBrandSystem() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(css)
                .contains("--font-serif: Georgia")
                .contains(".logo__mark")
                .contains(".product-card__cta")
                .contains(".info-list")
                .containsPattern("(?s)\\.logo__mark\\s*\\{"
                        + "(?=[^}]*width:\\s*32px)"
                        + "(?=[^}]*height:\\s*32px)"
                        + "(?=[^}]*background:\\s*var\\(--color-primary\\))"
                        + "[^}]*}");
    }

    @Test
    void publicHeadingsUseRestrainedBrandScale() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(css)
                .contains("--page-title-size: clamp(1.375rem, 1.2rem + 0.5vw, 1.625rem)")
                .contains("--article-title-size: clamp(1.75rem, 1.55rem + 1vw, 2.25rem)")
                .containsPattern("(?s)\\.page-heading\\s+h1\\s*\\{"
                        + "[^}]*font-size:\\s*var\\(--page-title-size\\)[^}]*}")
                .containsPattern("(?s)\\.article-heading\\s+h1\\s*\\{"
                        + "[^}]*font-size:\\s*var\\(--article-title-size\\)[^}]*}");
    }

    @Test
    void contentPagesUseBrandRhythm() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(css)
                .containsPattern("(?s)\\.article-card\\s*\\{"
                        + "(?=[^}]*display:\\s*flex)"
                        + "(?=[^}]*flex-direction:\\s*column)"
                        + "(?=[^}]*gap:\\s*var\\(--space-md\\))"
                        + "[^}]*}")
                .containsPattern("(?s)\\.article-heading\\s+\\.meta\\s*\\{"
                        + "[^}]*margin-top:\\s*var\\(--space-sm\\)[^}]*}")
                .containsPattern("(?s)\\.article-body\\s*>\\s*:\\s*first-child\\s*\\{"
                        + "[^}]*margin-top:\\s*0[^}]*}")
                .containsPattern("(?s)\\.article-body\\s*>\\s*:\\s*last-child\\s*\\{"
                        + "[^}]*margin-bottom:\\s*0[^}]*}")
                .containsPattern("(?s)\\.article-body\\s+p\\s*\\{"
                        + "[^}]*margin-block:\\s*var\\(--space-md\\)[^}]*}")
                .containsPattern("(?s)\\.error-card\\s+\\.button\\s*\\{"
                        + "[^}]*margin-top:\\s*var\\(--space-lg\\)[^}]*}");
    }
}
