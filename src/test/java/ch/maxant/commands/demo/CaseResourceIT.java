package ch.maxant.commands.demo;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import io.undertow.util.StatusCodes;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class CaseResourceIT {

    @Test
    public void testGetCase() throws Exception {

        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.setBaseUri(getBaseUriForLocalhost());
        builder.setAccept(ContentType.JSON);
        RequestSpecification spec = builder.build();

        given(spec)
                .when()
                .get("/demo/cases/case/1")
                .then()
                .log().body()
                .statusCode(StatusCodes.OK)
                .body("nr", is(1))
                .body("description", is("It started to hail and then..."));
    }

    public String getBaseUriForLocalhost() {
        return "http://localhost:" + (8080 + Integer.getInteger("swarm.port.offset", 1));
    }
}
