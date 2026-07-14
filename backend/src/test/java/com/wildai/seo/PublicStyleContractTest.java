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
}
