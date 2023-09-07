package io.openshift.booster.service;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

//This class provides a RESTful API with a /run method that can be triggered 
//through the main app webpage. It is based on Quarkus run time. You don't need an Application to run it 
//just time mvn compile quarkus:dev

@Path("/my_robot")
public class RobotEndpoint {

    // This contains the uri of the Robot APi that this application is invoking. The
    // value is defined in application.properties.
    // Set it to your specific API
    @ConfigProperty(name = "hub.controller.uri")
    String hubControllerEndpoint;

    CloseableHttpClient httpclient = HttpClients.createDefault();

    // This method checks if the HubController can be reached

    // This contains the uri of the Robot APi that this application is invoking. The
    // value is defined in application.properties.
    // Set it to your specific API
    @GET
    public Object ping() throws ClientProtocolException, IOException {

        System.out.println("Ping method invoked");
        String response = "Ping method invoked";

        CloseableHttpResponse httpResponse = httpclient.execute(new HttpGet(hubControllerEndpoint));

        try {

            String responseString = new BasicResponseHandler().handleResponse(httpResponse);
            System.out.println(responseString);

            HttpEntity entity1 = httpResponse.getEntity();
            EntityUtils.consume(entity1);
        } finally {
            httpResponse.close();
        }

        return response;
    }

    // This method should execute the program steps for the robot. It can be invoked
    // by the main application website
    @POST
    @Path("/run")
    public Object run() throws ClientProtocolException, IOException {

        System.out.println("Run method invoked");

        String response = "Run method invoked";

        // Example GET invokation of the Robot API

        /*
         * CloseableHttpResponse httpResponse = httpclient.execute(new
         * HttpGet(hubControllerEndpoint + "/distance"));
         * 
         * try {
         * 
         * response = new BasicResponseHandler().handleResponse(httpResponse);
         * System.out.println(response);
         * 
         * HttpEntity entity1 = httpResponse.getEntity();
         * EntityUtils.consume(entity1);
         * } finally {
         * httpResponse.close();
         * }
         */

        // Example POST invokation of the Robot API

        /*
         * HttpPost httpPost = new HttpPost(hubControllerEndpoint + "/forward/5");
         * List<NameValuePair> nvps = new ArrayList<NameValuePair>();
         * nvps.add(new BasicNameValuePair("userKey", "<USERKEY>"));
         * httpPost.setEntity(new UrlEncodedFormEntity(nvps));
         * CloseableHttpResponse httpResponse = httpclient.execute(httpPost);
         * 
         * try {
         * response = new BasicResponseHandler().handleResponse(httpResponse);
         * System.out.println(response);
         * HttpEntity entity = httpResponse.getEntity();
         * 
         * EntityUtils.consume(entity);
         * } finally {
         * httpResponse.close();
         * }
         * 
         * return response;
         */
    }

}