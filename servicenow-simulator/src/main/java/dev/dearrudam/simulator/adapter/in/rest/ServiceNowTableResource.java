package dev.dearrudam.simulator.adapter.in.rest;

import dev.dearrudam.simulator.adapter.store.SimulatedIncidentStore;
import dev.dearrudam.simulator.domain.model.SimulatedIncident;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Simulates the ServiceNow Table API for incidents.
 * Returns incidents in the ServiceNow envelope format: { "result": [...] }
 */
@Path("/api/now/table/incident")
@Produces(MediaType.APPLICATION_JSON)
public class ServiceNowTableResource {

    @Inject
    SimulatedIncidentStore store;

    @GET
    public Response list() {
        List<Map<String, String>> result = store.getAll().stream()
                .map(ServiceNowTableResource::toServiceNowRecord)
                .toList();
        return Response.ok(Map.of("result", result)).build();
    }

    private static Map<String, String> toServiceNowRecord(SimulatedIncident incident) {
        return Map.of(
                "sys_id", UUID.randomUUID().toString(),
                "short_description", incident.title(),
                "description", incident.description(),
                "category", incident.domain());
    }
}
