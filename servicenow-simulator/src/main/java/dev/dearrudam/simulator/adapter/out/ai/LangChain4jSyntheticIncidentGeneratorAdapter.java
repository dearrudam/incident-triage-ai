package dev.dearrudam.simulator.adapter.out.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.dearrudam.simulator.domain.model.SimulatedIncident;
import dev.dearrudam.simulator.domain.port.SyntheticIncidentGeneratorPort;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Outbound AI adapter that implements {@link SyntheticIncidentGeneratorPort}
 * using a LangChain4j {@code @RegisterAiService} interface backed by Ollama.
 *
 * <p>The {@link SyntheticIncidentAiService} inner interface returns the raw JSON
 * string from the LLM. The adapter parses it into a list of
 * {@link SimulatedIncident} domain objects using Jackson.
 *
 * <p>Returning {@code String} from the AI service avoids the limitations of
 * {@code PojoCollectionOutputParser} with Java records and gives full control
 * over deserialization.
 */
@ApplicationScoped
public class LangChain4jSyntheticIncidentGeneratorAdapter implements SyntheticIncidentGeneratorPort {

    private static final Logger LOG = Logger.getLogger(LangChain4jSyntheticIncidentGeneratorAdapter.class);

    /**
     * LangChain4j AI service interface for generating synthetic incidents.
     * Returns raw JSON string to allow flexible Jackson-based deserialization.
     */
    @RegisterAiService
    interface SyntheticIncidentAiService {

        @SystemMessage("""
                You are a helpful assistant that generates realistic IT incident scenarios for testing purposes.
                Your task is to produce a list of plausible IT incidents for the given domain.

                Always respond with a JSON array. Each element must be a JSON object with exactly these three fields:
                - "title": a short, descriptive incident title (max 100 characters)
                - "description": a detailed description of the incident (1-3 sentences)
                - "domain": the IT domain of the incident (must match the requested domain)

                Respond with ONLY the raw JSON array — no markdown, no code fences, no extra text.
                """)
        @UserMessage("""
                Generate {count} realistic IT incident(s) for the following domain: {domain}.

                Return a JSON array with exactly {count} element(s).
                """)
        String generate(int count, String domain);
    }

    @Inject
    SyntheticIncidentAiService aiService;

    @Inject
    ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     *
     * <p>Calls the LLM via {@link SyntheticIncidentAiService}, then parses the
     * returned JSON string into a list of {@link SimulatedIncident} domain objects.
     * Returns an empty list if parsing fails.
     *
     * <p>Some models (e.g. gemma3) wrap the JSON in markdown code fences even when
     * instructed not to. {@link #stripMarkdownCodeFences(String)} removes those fences
     * before deserialization so the adapter is robust to this common LLM quirk.
     */
    @Override
    public List<SimulatedIncident> generate(int count, String domain) {
        String raw = aiService.generate(count, domain);
        String json = stripMarkdownCodeFences(raw);
        try {
            List<IncidentDto> dtos = objectMapper.readValue(json,
                    new TypeReference<List<IncidentDto>>() {
                    });
            return dtos.stream()
                    .map(dto -> new SimulatedIncident(dto.title(), dto.description(), dto.domain()))
                    .toList();
        } catch (Exception e) {
            LOG.errorf("Failed to parse LLM response into incidents: %s | raw response: %s",
                    e.getMessage(), raw);
            return Collections.emptyList();
        }
    }

    /**
     * Strips markdown code fences (e.g. {@code ```json ... ```}) that some models
     * include in responses despite being asked to return plain JSON.
     *
     * @param text the raw LLM output
     * @return the trimmed content between the fences, or the original text if no fences are found
     */
    static String stripMarkdownCodeFences(String text) {
        if (text == null) return null;
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                String afterFence = trimmed.substring(firstNewline + 1);
                int closingFence = afterFence.lastIndexOf("```");
                if (closingFence != -1) {
                    return afterFence.substring(0, closingFence).strip();
                }
            }
        }
        return trimmed;
    }

    /**
     * Internal DTO for Jackson deserialization of LLM output.
     * Uses a record for brevity; lives in the adapter layer only.
     */
    private record IncidentDto(String title, String description, String domain) {
    }
}
