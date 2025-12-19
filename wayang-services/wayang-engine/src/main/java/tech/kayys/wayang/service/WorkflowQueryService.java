package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.exception.WorkflowNotFoundException;
import tech.kayys.wayang.mapper.WorkflowMapper;
import tech.kayys.wayang.model.PageResult;
import tech.kayys.wayang.repository.WorkflowRepository;
import tech.kayys.wayang.schema.PageInfo;
import tech.kayys.wayang.schema.PageInput;
import tech.kayys.wayang.schema.WorkflowConnection;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.schema.WorkflowDiffDTO;
import tech.kayys.wayang.schema.WorkflowFilterInput;

/**
 * WorkflowQueryService - Handles read operations
 */
@ApplicationScoped
public class WorkflowQueryService {

    private static final Logger LOG = Logger.getLogger(WorkflowQueryService.class);

    @Inject
    WorkflowRepository repository;

    @Inject
    WorkflowMapper mapper;

    @Inject
    ErrorHandlerService errorHandler;

    /**
     * Find workflow by ID
     */
    public Uni<WorkflowDTO> findById(String id, String tenantId) {
        LOG.debugf("Finding workflow: id=%s, tenant=%s", id, tenantId);

        return repository.findByIdAndTenant(UUID.fromString(id), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + id))
                .map(mapper::toDTO)
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to find workflow: %s", id);
                    return errorHandler.handleQueryError(throwable, id, tenantId);
                });
    }

    /**
     * Find all workflows with filtering and pagination
     */
    public Uni<WorkflowConnection> findAll(String tenantId,
            WorkflowFilterInput filter,
            PageInput page) {

        LOG.debugf("Finding workflows: tenant=%s, filter=%s", tenantId, filter);

        return repository.findAllByTenant(tenantId, filter, page)
                .map(result -> {
                    WorkflowConnection connection = new WorkflowConnection();
                    connection.setNodes(result.getContent().stream()
                            .map(mapper::toDTO)
                            .toList());
                    connection.setTotalCount((int) result.getTotalCount());
                    connection.setPageInfo(buildPageInfo(result, page));
                    return connection;
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to find workflows");
                    return errorHandler.handleListError(throwable, tenantId, filter);
                });
    }

    /**
     * Compare two workflows
     */
    public Uni<WorkflowDiffDTO> compareWorkflows(String baseId, String targetId,
            String tenantId) {

        LOG.infof("Comparing workflows: base=%s, target=%s", baseId, targetId);

        return Uni.combine().all()
                .unis(
                        repository.findByIdAndTenant(UUID.fromString(baseId), tenantId),
                        repository.findByIdAndTenant(UUID.fromString(targetId), tenantId))
                .asTuple()
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("One or both workflows not found"))
                .map(tuple -> {
                    Workflow base = tuple.getItem1();
                    Workflow target = tuple.getItem2();

                    // Perform diff
                    return performDiff(base, target);
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to compare workflows");
                    return errorHandler.handleCompareError(
                            throwable, baseId, targetId, tenantId);
                });
    }

    private WorkflowDiffDTO performDiff(Workflow base, Workflow target) {
        // Implementation delegated to DiffService
        // This is a simplified version
        WorkflowDiffDTO diff = new WorkflowDiffDTO();
        diff.setBaseId(base.id.toString());
        diff.setTargetId(target.id.toString());
        diff.setBaseVersion(base.version);
        diff.setTargetVersion(target.version);
        // ... diff logic
        return diff;
    }

    private PageInfo buildPageInfo(PageResult<?> result, PageInput page) {
        PageInfo info = new PageInfo();
        info.setHasNextPage(result.hasNextPage());
        info.setHasPreviousPage(result.hasPreviousPage());
        return info;
    }

    public Uni<List<WorkflowDTO>> findVersions(String id, String tenantId) {
        return Uni.createFrom().item(List.of());
    }
}
