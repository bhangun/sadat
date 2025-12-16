package tech.kayys.wayang.service;

import java.util.Optional;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.domain.WorkflowDraft;
import tech.kayys.wayang.repository.WorkflowDraftRepository;
import tech.kayys.wayang.tenant.TenantContext;

/**
 * DraftService - Auto-save draft management
 */
@ApplicationScoped
public class DraftService {

    private static final Logger LOG = Logger.getLogger(DraftService.class);
    private static final int MAX_DRAFTS_PER_USER = 10;

    @Inject
    WorkflowDraftRepository draftRepository;

    @Inject
    TenantContext tenantContext;

    /**
     * Save draft
     */
    @Transactional
    public Uni<WorkflowDraft> saveDraft(UUID workflowId,
            WorkflowDraft.WorkflowSnapshot snapshot, boolean autoSave) {
        String userId = tenantContext.getUserId();

        return draftRepository.saveDraft(workflowId, userId, snapshot, autoSave)
                .invoke(draft -> LOG.debugf("Draft saved for workflow %s, user %s",
                        workflowId, userId))
                .flatMap(draft -> draftRepository.cleanOldDrafts(workflowId, userId, MAX_DRAFTS_PER_USER)
                        .replaceWith(draft));
    }

    /**
     * Get latest draft
     */
    public Uni<Optional<WorkflowDraft>> getLatestDraft(UUID workflowId) {
        String userId = tenantContext.getUserId();

        return draftRepository.findLatestDraft(workflowId, userId)
                .map(Optional::ofNullable);
    }
}