package com.camp.reservations.service;

import com.camp.reservations.exception.DescriptionGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DescriptionGenerationService {

    private static final String DISCLAIMER = " (AI generated description)";
    private static final String GROQ_MODEL = "openai/gpt-oss-120b";

    private final RestClient groqClient;
    private final WebSearchService webSearchService;
    private final String groqApiKey;

    public DescriptionGenerationService(WebSearchService webSearchService,
                                         @Value("${groq.api-key:}") String groqApiKey) {
        this.groqClient = RestClient.builder().baseUrl("https://api.groq.com/openai/v1").build();
        this.webSearchService = webSearchService;
        this.groqApiKey = groqApiKey;
    }

    public String generateDescription(String campsiteName) {
        if (!StringUtils.hasText(groqApiKey)) {
            throw new DescriptionGenerationException(
                    "AI description generation isn't configured. Set GROQ_API_KEY on the server "
                            + "(free key at console.groq.com).");
        }

        String searchResults = webSearchService.search(campsiteName + " campground camping");
        String prompt = buildPrompt(campsiteName, searchResults);

        JsonNode response;
        try {
            response = groqClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .body(Map.of(
                            "model", GROQ_MODEL,
                            "temperature", 0.6,
                            "max_tokens", 500,
                            "reasoning_effort", "low",
                            "messages", List.of(Map.of("role", "user", "content", prompt))
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.warn("Groq API call failed", ex);
            throw new DescriptionGenerationException(
                    "Couldn't reach the AI description service. Check that GROQ_API_KEY is valid and try again.", ex);
        }

        String description = extractText(response);
        if (!StringUtils.hasText(description)) {
            throw new DescriptionGenerationException(
                    "The AI didn't return a description. Please try again or write one manually.");
        }

        return stripSurroundingQuotes(description.trim()) + DISCLAIMER;
    }

    private String buildPrompt(String campsiteName, String searchResults) {
        String context = StringUtils.hasText(searchResults)
                ? "Here are some web search results about it:\n" + searchResults
                : "No reliable web search results were found for this exact name.";

        return """
                A camp owner is listing a campsite called "%s" on a booking website.
                %s

                Write an inviting, factual description of exactly 50 words based on the search results above,
                if they are relevant. If the search results are missing or not clearly about this exact place,
                write a plausible, appealing 50-word description for a campsite with this name instead, without
                inventing specific false facts (no invented lake, mountain, or landmark names).
                Do not wrap the text in quotes, do not use markdown, and do not start with the campsite's name.
                Reply with only the 50-word description, nothing else.
                """.formatted(campsiteName, context);
    }

    private String extractText(JsonNode response) {
        if (response == null) {
            return "";
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? "" : content.asString("");
    }

    private String stripSurroundingQuotes(String text) {
        if (text.length() >= 2
                && (text.charAt(0) == '"' || text.charAt(0) == '“')
                && (text.charAt(text.length() - 1) == '"' || text.charAt(text.length() - 1) == '”')) {
            return text.substring(1, text.length() - 1).trim();
        }
        return text;
    }
}
