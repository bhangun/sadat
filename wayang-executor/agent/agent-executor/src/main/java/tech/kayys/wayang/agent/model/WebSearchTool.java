package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Web Search Tool
 * Searches the web for information
 */
@ApplicationScoped
public class WebSearchTool extends AbstractTool {

        private static final Logger LOG = LoggerFactory.getLogger(WebSearchTool.class);

        public WebSearchTool() {
                super("web_search", "Searches the web for current information. " +
                                "Use this when you need up-to-date information or facts.");
        }

        @Override
        public Map<String, Object> parameterSchema() {
                return Map.of(
                                "type", "object",
                                "properties", Map.of(
                                                "query", Map.of(
                                                                "type", "string",
                                                                "description", "Search query"),
                                                "num_results", Map.of(
                                                                "type", "integer",
                                                                "description", "Number of results to return",
                                                                "default", 5)),
                                "required", List.of("query"));
        }

        @Override
        public Uni<String> execute(Map<String, Object> arguments, AgentContext context) {
                String query = getParam(arguments, "query", String.class);
                Integer numResults = getParamOrDefault(arguments, "num_results", 5);

                LOG.debug("Searching web: {} (limit: {})", query, numResults);

                return performWebSearch(query, numResults)
                                .map(results -> formatSearchResults(results));
        }

        private Uni<List<SearchResult>> performWebSearch(String query, int limit) {
                // In production, integrate with actual search API
                // (Google Custom Search, Bing API, etc.)

                // Placeholder implementation
                return Uni.createFrom().item(List.of(
                                new SearchResult(
                                                "Example Result",
                                                "https://example.com",
                                                "This is an example search result for: " + query)));
        }

        private String formatSearchResults(List<SearchResult> results) {
                if (results.isEmpty()) {
                        return "No results found.";
                }

                return results.stream()
                                .map(r -> String.format("Title: %s\nURL: %s\nSnippet: %s",
                                                r.title(), r.url(), r.snippet()))
                                .collect(Collectors.joining("\n\n"));
        }

        private record SearchResult(String title, String url, String snippet) {
        }
}