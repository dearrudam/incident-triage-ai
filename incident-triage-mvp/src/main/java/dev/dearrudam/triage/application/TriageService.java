package dev.dearrudam.triage.application;

import dev.dearrudam.triage.adapter.out.db.HistoricalIncidentRepository;
import dev.dearrudam.triage.domain.model.HistoricalIncident;
import dev.dearrudam.triage.domain.model.TriageRequest;
import dev.dearrudam.triage.domain.model.TriageResponse;
import dev.dearrudam.triage.domain.port.EmbeddingGeneratorPort;
import dev.dearrudam.triage.domain.port.TriageDecisionPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Application service that orchestrates the triage flow:
 * 1. Generate embedding for incoming request
 * 2. Find similar historical incidents via vector search
 * 3. Produce a triage decision via LLM
 */
@ApplicationScoped
public class TriageService {

    private static final int SIMILAR_INCIDENTS_LIMIT = 5;

    @Inject
    EmbeddingGeneratorPort embeddingGeneratorPort;

    @Inject
    TriageDecisionPort triageDecisionPort;

    @Inject
    HistoricalIncidentRepository historicalIncidentRepository;

    public TriageResponse triage(TriageRequest request) {
        String text = request.title() + " " + request.description();
        List<Float> embedding = embeddingGeneratorPort.generate(text);
        List<HistoricalIncident> similarIncidents = historicalIncidentRepository.findSimilar(embedding,
                SIMILAR_INCIDENTS_LIMIT);
        return triageDecisionPort.decide(request, similarIncidents);
    }
}
