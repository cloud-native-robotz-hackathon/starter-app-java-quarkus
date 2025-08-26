package com.redhat.robotics.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "robot-api")
@Path("/")
public interface RobotApiClient {

    @GET
    @Path("/camera")
    @Produces(MediaType.TEXT_PLAIN)
    String getCamera(@QueryParam("user_key") String userKey);

    @POST
    @Path("/forward/{length}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void moveForward(@PathParam("length") int length, @FormParam("user_key") String userKey);

    @POST
    @Path("/backward/{length}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void moveBackward(@PathParam("length") int length, @FormParam("user_key") String userKey);

    @POST
    @Path("/left/{degrees}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void turnLeft(@PathParam("degrees") int degrees, @FormParam("user_key") String userKey);

    @POST
    @Path("/right/{degrees}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    void turnRight(@PathParam("degrees") int degrees, @FormParam("user_key") String userKey);

    @GET
    @Path("/distance")
    @Produces(MediaType.TEXT_PLAIN)
    String getDistance(@QueryParam("user_key") String userKey);

    @GET
    @Path("/remote_status")
    @Produces(MediaType.TEXT_PLAIN)
    String getRemoteStatus(@QueryParam("user_key") String userKey);
}
