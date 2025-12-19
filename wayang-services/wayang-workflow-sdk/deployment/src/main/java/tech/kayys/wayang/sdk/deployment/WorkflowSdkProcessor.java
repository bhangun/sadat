package tech.kayys.wayang.sdk.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import tech.kayys.wayang.sdk.WorkflowSDK;
import tech.kayys.wayang.sdk.dto.WorkflowRunResponse;
import tech.kayys.wayang.sdk.dto.TriggerWorkflowRequest;

class WorkflowSdkProcessor {

    private static final String FEATURE = "wayang-workflow-sdk";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection() {
        // Register SDK classes and DTOs for reflection (needed for JSON serialization in native mode)
        return ReflectiveClassBuildItem.builder(
            WorkflowSDK.class,
            WorkflowRunResponse.class,
            TriggerWorkflowRequest.class
        ).methods().fields().build();
    }
}
