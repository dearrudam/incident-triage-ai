package dev.dearrudam.simulator.adapter.out.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * MicroProfile REST client interface for the incident-triage-mvp service.
 * Configured via {@code quarkus.rest-client.triage-service.url} in application.properties.
 */
@RegisterRestClient(configKey = "triage-service")
@Path("/triage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface TriageServiceClient {

    /**
     * Submits an incident for AI-based triage.
     *
     * @param request the triage request containing the incident title and description
     * @return the triage response with priority, category, suggested team, and rationale
     */
    @POST
    TriageResponseDto triage(TriageRequestDto request);

    record TriageRequestDto(String title, String description) {
    }

    record TriageResponseDto(String priority, String category, String suggestedTeam, String rationale) {
    }
}
