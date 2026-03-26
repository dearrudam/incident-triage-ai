package dev.dearrudam.triage.domain.port;

import dev.dearrudam.triage.domain.model.HistoricalIncident;
import dev.dearrudam.triage.domain.model.TriageRequest;
import dev.dearrudam.triage.domain.model.TriageResponse;

import java.util.List;

/**
 * Port for producing a triage decision via an LLM.
 * The domain layer depends only on this interface — no LangChain4j imports
 * here.
 */
public interface TriageDecisionPort {

    /**
     * Decides the triage classification for an incoming incident.
     *
     * @param request          the incoming triage request
     * @param similarIncidents similar historical incidents retrieved via vector
     *                         search
     * @return a structured triage decision
     */
    TriageResponse decide(TriageRequest request, List<HistoricalIncident> similarIncidents);
}
