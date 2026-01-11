package tech.kayys.wayang.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import tech.kayys.wayang.domain.CanvasDefinition;

@QuarkusTest
public class CanvasToWorkflowConverterTest {

    @Inject
    CanvasToWorkflowConverter converter;

    @Test
    public void testConvertEmptyCanvas() {
        CanvasDefinition canvas = new CanvasDefinition();
        canvas.canvasData = new tech.kayys.wayang.canvas.schema.CanvasData();
        canvas.canvasData.nodes = new java.util.ArrayList<>();
        canvas.canvasData.edges = new java.util.ArrayList<>();
        canvas.metadata = new tech.kayys.wayang.canvas.schema.CanvasMetadata();
        canvas.metadata.labels = new java.util.HashMap<>();
        canvas.metadata.customFields = new java.util.HashMap<>();

        tech.kayys.silat.model.WorkflowDefinition workflowDef = (tech.kayys.silat.model.WorkflowDefinition) converter
                .convert(canvas, "Test Workflow", "1.0.0")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted()
                .getItem();

        assertNotNull(workflowDef);
        assertEquals("Test Workflow", workflowDef.name());
    }
}
