package com.wildai.seo;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PublicStyleContractTest {

    @Test
    void colorVariablesKeepSafariFallbackOutsideOklchSupports() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        int fallbackRootStart = css.indexOf(":root");
        String fallbackRoot = extractBlockBody(css, fallbackRootStart);
        int supportsStart = css.indexOf("@supports (color: oklch(0 0 0))");

        assertThat(fallbackRoot)
                .contains(
                        "--color-primary: #1f6b56;",
                        "--color-primary-hover: #195647;",
                        "--color-primary-muted: #edf4f2;",
                        "--color-primary-subtle: #f6faf8;",
                        "--color-bg: #f7f5f0;",
                        "--color-surface: #fdfcf9;",
                        "--color-surface-raised: #fffdfa;",
                        "--color-border: #e8e4dc;",
                        "--color-border-strong: #cec8be;",
                        "--color-text: #3d3a36;",
                        "--color-text-heading: #1f1d1a;",
                        "--color-text-muted: #6b6560;",
                        "--color-text-subtle: #8a837c;",
                        "--color-warning: #9a641b;",
                        "--color-warning-bg: #fff7e7;",
                        "--color-neutral: #807a73;",
                        "--color-neutral-bg: #f0ede7;",
                        "--shadow-sm: 0 1px 2px rgb(31 29 26 / 6%);",
                        "--shadow-md: 0 4px 12px rgb(31 29 26 / 8%);",
                        "--shadow-lg: 0 8px 24px rgb(31 29 26 / 10%);")
                .doesNotContain("oklch(");
        assertThat(supportsStart).isGreaterThan(fallbackRootStart);

        String supportsBody = extractBlockBody(css, supportsStart);
        int enhancedRootStart = supportsBody.indexOf(":root");
        String enhancedRoot = extractBlockBody(supportsBody, enhancedRootStart);
        assertThat(enhancedRoot)
                .contains(
                        "--color-primary: oklch(0.42 0.09 165);",
                        "--color-primary-hover: oklch(0.36 0.09 165);",
                        "--color-primary-muted: oklch(0.94 0.025 165);",
                        "--color-primary-subtle: oklch(0.97 0.012 165);",
                        "--color-bg: oklch(0.98 0.008 85);",
                        "--color-surface: oklch(0.995 0.005 85);",
                        "--color-surface-raised: oklch(1 0.003 85);",
                        "--color-border: oklch(0.9 0.012 85);",
                        "--color-border-strong: oklch(0.82 0.015 85);",
                        "--color-text: oklch(0.38 0.02 85);",
                        "--color-text-heading: oklch(0.22 0.025 85);",
                        "--color-text-muted: oklch(0.52 0.015 85);",
                        "--color-text-subtle: oklch(0.62 0.012 85);",
                        "--color-warning: oklch(0.55 0.14 65);",
                        "--color-warning-bg: oklch(0.96 0.04 75);",
                        "--color-neutral: oklch(0.55 0.01 85);",
                        "--color-neutral-bg: oklch(0.94 0.008 85);",
                        "--shadow-sm: 0 1px 2px oklch(0.2 0.02 85 / 0.06);",
                        "--shadow-md: 0 4px 12px oklch(0.2 0.02 85 / 0.08);",
                        "--shadow-lg: 0 8px 24px oklch(0.2 0.02 85 / 0.1);");
    }

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
                .containsPattern("(?s)\\.article-body\\s+h1,"
                        + "\\s*\\.article-body\\s+h2,"
                        + "\\s*\\.article-body\\s+h3,"
                        + "\\s*\\.article-body\\s+h4,"
                        + "\\s*\\.article-body\\s+h5,"
                        + "\\s*\\.article-body\\s+h6\\s*\\{"
                        + "[^}]*margin-block:\\s*var\\(--space-lg\\)[^}]*}")
                .containsPattern("(?s)\\.article-body\\s+h1,"
                        + "\\s*\\.article-body\\s+h2\\s*\\{"
                        + "[^}]*font-size:\\s*1\\.375rem[^}]*}")
                .containsPattern("(?s)\\.card-image,"
                        + "\\s*\\.article-cover\\s*\\{"
                        + "(?=[^}]*aspect-ratio:\\s*16\\s*/\\s*9)"
                        + "(?=[^}]*object-fit:\\s*cover)"
                        + "[^}]*}")
                .containsPattern("(?s)\\.error-card\\s+\\.button\\s*\\{"
                        + "[^}]*margin-top:\\s*var\\(--space-lg\\)[^}]*}");
    }

    private String extractBlockBody(String source, int blockStart) {
        assertThat(blockStart).as("应存在待检查的 CSS 规则").isGreaterThanOrEqualTo(0);
        int openingBrace = source.indexOf('{', blockStart);
        assertThat(openingBrace).as("CSS 规则应包含左花括号").isGreaterThan(blockStart);

        int depth = 1;
        for (int index = openingBrace + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openingBrace + 1, index);
                }
            }
        }
        throw new AssertionError("CSS 规则缺少右花括号");
    }
}
