package com.wildai.content.service;

import com.wildai.content.dto.RenderedArticleContent;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleMarkdownService {

    private static final int GENERATED_SUMMARY_LIMIT = 220;
    private static final int MANUAL_SUMMARY_LIMIT = 500;
    private static final String URL_VALIDATION_BASE_URI = "https://relative.invalid";
    private static final List<Extension> EXTENSIONS = List.of(TablesExtension.create());

    private final Parser parser = Parser.builder()
            .extensions(EXTENSIONS)
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .build();
    private final Safelist safelist = Safelist.relaxed()
            .addTags("table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("a", "rel")
            .removeProtocols("a", "href", "ftp")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    public RenderedArticleContent render(String markdown, String manualSummary) {
        String source = markdown == null ? "" : markdown;
        String unsafeHtml = renderer.render(parser.parse(source));
        String safeHtml = Jsoup.clean(unsafeHtml, URL_VALIDATION_BASE_URI, safelist,
                new Document.OutputSettings().prettyPrint(false));

        Document document = Jsoup.parseBodyFragment(safeHtml);
        document.select("a[href]").attr("rel", "noopener noreferrer");
        String html = document.body().html();
        String plainText = normalizeWhitespace(document.text());
        String summary = manualSummary == null || manualSummary.isBlank()
                ? clip(plainText, GENERATED_SUMMARY_LIMIT)
                : clip(normalizeWhitespace(manualSummary), MANUAL_SUMMARY_LIMIT);

        return new RenderedArticleContent(html, plainText, summary);
    }

    public String preview(String markdown) {
        return render(markdown, "").html();
    }

    private String normalizeWhitespace(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        int endIndex = max;
        if (Character.isHighSurrogate(value.charAt(endIndex - 1))
                && Character.isLowSurrogate(value.charAt(endIndex))) {
            endIndex--;
        }
        return value.substring(0, endIndex).stripTrailing();
    }
}
