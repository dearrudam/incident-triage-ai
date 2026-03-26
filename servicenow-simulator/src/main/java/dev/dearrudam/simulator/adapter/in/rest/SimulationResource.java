package dev.dearrudam.simulator.adapter.in.rest;

import dev.dearrudam.simulator.adapter.out.rest.TriageServiceClient;
import dev.dearrudam.simulator.adapter.store.SimulatedIncidentStore;
import dev.dearrudam.simulator.domain.model.SimulatedIncident;
import dev.dearrudam.simulator.domain.port.SyntheticIncidentGeneratorPort;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST adapter for simulation control endpoints.
 * POST /simulation/generate — generate synthetic incidents via LLM
 * POST /simulation/dispatch — dispatch stored incidents to the triage service
 */
@Path("/simulation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SimulationResource {

    private static final Logger LOG = Logger.getLogger(SimulationResource.class);

    @Inject
    SyntheticIncidentGeneratorPort generatorPort;

    @Inject
    SimulatedIncidentStore store;

    @RestClient
    TriageServiceClient triageServiceClient;

    // --- /simulation/generate ---

    @POST
    @Path("/generate")
    public Response generate(GenerateRequest body) {
        int count = switch (body) {
            case GenerateRequest(Integer c, _) when c != null -> c;
            default -> 3;
        };
        String domain = switch (body) {
            case GenerateRequest(_, String d) when d != null && !d.isBlank() -> d;
            default -> "general";
        };

        LOG.infof("Generating %d synthetic incidents for domain: %s", count, domain);
        List<SimulatedIncident> incidents = generatorPort.generate(count, domain);
        store.replace(incidents);

        return Response.ok(Map.of(
                "generated", incidents.size(),
                "domain", domain)).build();
    }

    // --- /simulation/dispatch ---

    @POST
    @Path("/dispatch")
    public Response dispatch() {
        List<SimulatedIncident> incidents = store.getAll();

        if (incidents.isEmpty()) {
            return Response.ok(Map.of("dispatched", 0, "results", List.of())).build();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (SimulatedIncident incident : incidents) {
            try {
                TriageServiceClient.TriageResponseDto response = triageServiceClient.triage(
                        new TriageServiceClient.TriageRequestDto(
                                incident.title(), incident.description()));
                results.add(Map.of(
                        "incident", incident.title(),
                        "priority", response.priority(),
                        "category", response.category(),
                        "suggestedTeam", response.suggestedTeam(),
                        "rationale", response.rationale()));
            } catch (Exception e) {
                String errorMessage = e.getMessage() instanceof String msg ? msg : "unknown error";
                LOG.errorf("Failed to triage incident '%s': %s", incident.title(), errorMessage);
                results.add(Map.of(
                        "incident", incident.title(),
                        "error", errorMessage));
            }
        }

        return Response.ok(Map.of(
                "dispatched", incidents.size(),
                "results", results)).build();
    }

    public record GenerateRequest(
            @Min(1) @Max(20) Integer count,
            String domain) {
    }
}
