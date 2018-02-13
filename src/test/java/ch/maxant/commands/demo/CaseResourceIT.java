package ch.maxant.commands.demo;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import io.undertow.util.StatusCodes;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class CaseResourceIT {

    @Test
    public void testAll() throws Exception {

        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.setBaseUri(getBaseUriForLocalhost());
        builder.setAccept(ContentType.JSON);
        RequestSpecification spec = builder.build();

        String body = given(spec)
                .when()
                .get("/demo/cases/case/1")
                .then()
                .log().body()
                .statusCode(StatusCodes.OK)
                .body("nr", is(1))
                .body("description", is("It started to hail and then..."))
                .extract()
                .response()
                .body()
                .asString();

        given(spec)
                .body(body)
                .when()
                .header(new Header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .put("/demo/cases/case")
                .then()
                .log()
                .body()
                .statusCode(StatusCodes.NO_CONTENT);
    }

    public String getBaseUriForLocalhost() {
        return "http://localhost:" + (8080 + Integer.getInteger("swarm.port.offset", 1));
    }
}
