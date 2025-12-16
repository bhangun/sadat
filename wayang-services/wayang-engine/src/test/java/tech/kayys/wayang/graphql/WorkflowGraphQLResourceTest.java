package tech.kayys.wayang.graphql;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
public class WorkflowGraphQLResourceTest {

    @Test
    public void testSchemaValidity() {
        given()
                .when().get("/graphql/schema.graphql")
                .then()
                .statusCode(200)
                .contentType(ContentType.TEXT)
                .body(containsString("type Workflow"));
    }
}
