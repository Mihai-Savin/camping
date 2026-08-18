package com.camp.reservations.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class WebSearchService {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final int MAX_RESULTS = 4;

    /**
     * Best-effort, keyless web search via DuckDuckGo's HTML endpoint. Returns an empty
     * string (never throws) when the lookup fails or turns up nothing usable.
     */
    public String search(String query) {
        try {
            String url = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(6000)
                    .get();

            Elements results = doc.select("div.result");
            StringBuilder summary = new StringBuilder();
            int count = 0;
            for (Element result : results) {
                if (count >= MAX_RESULTS) {
                    break;
                }
                String title = result.select(".result__a").text();
                String snippet = result.select(".result__snippet").text();
                if (title.isBlank() && snippet.isBlank()) {
                    continue;
                }
                summary.append("- ").append(title);
                if (!snippet.isBlank()) {
                    summary.append(": ").append(snippet);
                }
                summary.append('\n');
                count++;
            }
            return summary.toString().trim();
        } catch (Exception ex) {
            log.warn("Web search lookup failed for query '{}': {}", query, ex.getMessage());
            return "";
        }
    }
}
