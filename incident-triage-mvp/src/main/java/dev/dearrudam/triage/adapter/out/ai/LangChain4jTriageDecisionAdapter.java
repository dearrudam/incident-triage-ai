package dev.dearrudam.triage.adapter.out.ai;

import dev.dearrudam.triage.domain.model.HistoricalIncident;
import dev.dearrudam.triage.domain.model.TriageRequest;
import dev.dearrudam.triage.domain.model.TriageResponse;
import dev.dearrudam.triage.domain.port.TriageDecisionPort;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Outbound AI adapter that implements {@link TriageDecisionPort} using a
 * LangChain4j {@code @RegisterAiService} interface backed by Ollama.
 *
 * <p>The {@link TriageAiService} inner interface defines the structured prompt
 * contract. LangChain4j Quarkus extension auto-generates the CDI proxy at
 * build time and deserialises the response into a {@link TriageResponse}
 * record.
 */
@ApplicationScoped
public class LangChain4jTriageDecisionAdapter implements TriageDecisionPort {

    /**
     * LangChain4j AI service interface for producing triage decisions.
     * Quarkus generates the CDI bean implementation at build time.
     */
    @RegisterAiService
    interface TriageAiService {

        @SystemMessage("""
                You are an expert IT incident triage assistant.
                Your task is to classify an incoming IT incident and suggest the team best suited to resolve it.

                Always respond with a JSON object containing exactly these four fields:
                - "priority": one of CRITICAL, HIGH, MEDIUM, LOW
                - "category": a short category label (e.g. Database, Network, Application, Security, Infrastructure)
                - "suggestedTeam": the team name most likely responsible for resolution
                - "rationale": a one or two sentence explanation of your decision

                Base your decision on the incident title, description, and any similar historical incidents provided.
                Respond with ONLY the raw JSON object — no markdown, no code fences, no extra text.
                """)
        @UserMessage("""
                === Incoming Incident ===
                Title: {title}
                Description: {description}

                === Similar Historical Incidents ===
                {historicalContext}

                Classify the incident and return a JSON triage decision.
                """)
        TriageResponse decide(String title, String description, String historicalContext);
    }

    @Inject
    TriageAiService triageAiService;

    /**
     * {@inheritDoc}
     *
     * <p>Formats the similar incidents list as a human-readable context block and
     * delegates to the {@link TriageAiService} LLM prompt.
     */
    @Override
    public TriageResponse decide(TriageRequest request, List<HistoricalIncident> similarIncidents) {
        String historicalContext = buildHistoricalContext(similarIncidents);
        return triageAiService.decide(request.title(), request.description(), historicalContext);
    }

    private static String buildHistoricalContext(List<HistoricalIncident> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return "No similar historical incidents found.";
        }
        return incidents.stream()
                .map(i -> String.format(
                        "- [%s / %s] %s: %s (resolved by: %s)",
                        i.priority(), i.category(), i.title(), i.description(), i.resolvedBy()))
                .collect(Collectors.joining("\n"));
    }
}
