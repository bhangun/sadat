package tech.kayys.wayang.repository;

import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.WorkflowDraft;

/**
 * WorkflowDraftRepository - Auto-save draft management
 */
@ApplicationScoped
public class WorkflowDraftRepository implements PanacheRepositoryBase<WorkflowDraft, UUID> {

        /**
         * Save draft snapshot
         */
        public Uni<WorkflowDraft> saveDraft(UUID workflowId, String userId,
                        WorkflowDraft.WorkflowSnapshot content, boolean autoSave) {
                WorkflowDraft draft = new WorkflowDraft();
                draft.workflowId = workflowId;
                draft.userId = userId;
                draft.content = content;
                draft.autoSaved = autoSave;

                return persist(draft);
        }

        /**
         * Find latest draft for user
         */
        public Uni<WorkflowDraft> findLatestDraft(UUID workflowId, String userId) {
                return find("workflowId = :workflowId and userId = :userId order by savedAt desc",
                                Parameters.with("workflowId", workflowId)
                                                .and("userId", userId))
                                .firstResult();
        }

        /**
         * Clean old drafts (keep last N)
         */
        public Uni<Long> cleanOldDrafts(UUID workflowId, String userId, int keepCount) {
                return find("workflowId = :workflowId and userId = :userId order by savedAt desc",
                                Parameters.with("workflowId", workflowId)
                                                .and("userId", userId))
                                .list()
                                .flatMap(drafts -> {
                                        if (drafts.size() <= keepCount) {
                                                return Uni.createFrom().item(0L);
                                        }

                                        List<UUID> toDelete = drafts.stream()
                                                        .skip(keepCount)
                                                        .map(d -> d.id)
                                                        .toList();

                                        return delete("id in :ids",
                                                        Parameters.with("ids", toDelete));
                                });
        }
}