package dev.dearrudam.triage.adapter.in.rest;

import dev.dearrudam.triage.application.TriageService;
import dev.dearrudam.triage.domain.model.TriageRequest;
import dev.dearrudam.triage.domain.model.TriageResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST inbound adapter exposing the triage endpoint.
 * Accepts an incident description and returns a structured triage decision.
 */
@Path("/triage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TriageResource {

    @Inject
    TriageService triageService;

    @POST
    public Response triage(@Valid TriageRequestBody body) {
        TriageRequest request = new TriageRequest(body.title(), body.description());
        TriageResponse response = triageService.triage(request);
        return Response.ok(response).build();
    }

    public record TriageRequestBody(
            @NotBlank(message = "title must not be blank") String title,
            @NotBlank(message = "description must not be blank") String description) {
    }
}
