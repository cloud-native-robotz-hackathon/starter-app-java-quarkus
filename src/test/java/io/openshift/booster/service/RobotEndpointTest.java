package io.openshift.booster.service;

import io.quarkus.test.junit.QuarkusTest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;




import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;





@QuarkusTest
public class RobotEndpointTest {

    @Test
    public void testPingEndpoint() {
        given().when().get("/my_robot").then().statusCode(200).body(is("Ping method invoked"));
    }

}
