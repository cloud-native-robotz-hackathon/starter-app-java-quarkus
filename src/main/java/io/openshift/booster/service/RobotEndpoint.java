package io.openshift.booster.service;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

//This class provides a RESTful API with a /run method that can be triggered 
//through the main app webpage. It is based on Quarkus run time. You don't need an Application to run it 
//just time mvn compile quarkus:dev

@Path("/my_robot")
public class RobotEndpoint {

    // This contains the uri of the Robot APi that this application is invoking. The
    // value is defined in application.properties.
    // Set it to your specific API
    @Value("${hub.controller.uri}")
    String hubControllerEndpoint;

    private RestTemplate restTemplate = new RestTemplate();

    // This method checks if the HubController can be reached
    @GET
    // This contains the uri of the Robot APi that this application is invoking. The
    // value is defined in application.properties.
    // Set it to your specific API
    @Value("${hub.controller.uri}")
    public Object ping() {

        System.out.println("Ping method invoked");
        String response= "Ping method invoked";
        //String response = restTemplate.getForObject(hubControllerEndpoint, String.class);
        return response;
    }

    // This method should execute the program steps for the robot. It can be invoked
    // by the main application website
    @POST
    @Path("/run")
    public Object run() {

        System.out.println("Run method invoked");

        String response = "Run method invoked";

        // Example GET invokation of the Robot API
        // response = restTemplate.getForObject(hubControllerEndpoint +
        // "/power?user_key=<YOUR USER KEY>", String.class);

        // Example POST invokation of the Robot API
        // MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<String,
        // String>();
        // paramMap.add("user_key", "<YOUR USER KEY>");
        // HttpEntity<MultiValueMap<String, String>> request = new
        // HttpEntity<MultiValueMap<String, String>>(paramMap,
        // new LinkedMultiValueMap<String, String>());
        // response = restTemplate.postForObject(hubControllerEndpoint + "/forward/5",
        // request, String.class);

        return response;
    }

}