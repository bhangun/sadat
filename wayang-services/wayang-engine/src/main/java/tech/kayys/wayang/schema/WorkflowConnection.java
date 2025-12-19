package tech.kayys.wayang.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * WorkflowConnection - Paginated result
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowConnection {

    private List<WorkflowDTO> nodes = new ArrayList<>();
    private PageInfo pageInfo;
    private int totalCount;

    // Getters and setters...
    public List<WorkflowDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<WorkflowDTO> nodes) {
        this.nodes = nodes;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
