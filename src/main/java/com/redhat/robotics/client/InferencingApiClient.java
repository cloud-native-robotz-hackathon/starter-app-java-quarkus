package com.redhat.robotics.client;

import com.redhat.robotics.model.InferencingRequest;
import com.redhat.robotics.model.InferencingResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "inferencing-api")
public interface InferencingApiClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    InferencingResponse predict(InferencingRequest request, @HeaderParam("Authorization") String authHeader);
}
