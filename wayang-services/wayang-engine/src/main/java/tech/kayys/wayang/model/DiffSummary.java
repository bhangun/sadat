package tech.kayys.wayang.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * DiffSummary - Aggregated summary of all changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiffSummary {

    // Logic changes
    public int nodesAdded = 0;
    public int nodesDeleted = 0;
    public int nodesModified = 0;
    public int connectionsAdded = 0;
    public int connectionsDeleted = 0;
    public int connectionsModified = 0;

    // UI changes
    public boolean uiChanged = false;
    public int nodePositionChanges = 0;
    public int nodeStyleChanges = 0;

    // Runtime changes
    public boolean runtimeChanged = false;

    // Metadata changes
    public boolean metadataChanged = false;

    // Totals
    public int totalChanges = 0;
    public int breakingChanges = 0;

    // Impact assessment
    public ChangeImpact overallImpact = ChangeImpact.NONE;
    public List<String> warnings = new ArrayList<>();
    public List<String> recommendations = new ArrayList<>();

    /**
     * Calculate totals
     */
    public void calculateTotals() {
        totalChanges = nodesAdded + nodesDeleted + nodesModified +
                connectionsAdded + connectionsDeleted + connectionsModified +
                (uiChanged ? 1 : 0) + (runtimeChanged ? 1 : 0) +
                (metadataChanged ? 1 : 0);

        breakingChanges = nodesDeleted + connectionsDeleted;

        // Determine overall impact
        if (breakingChanges > 0) {
            overallImpact = ChangeImpact.BREAKING;
        } else if (nodesModified > 0 || connectionsModified > 0 || runtimeChanged) {
            overallImpact = ChangeImpact.MAJOR;
        } else if (nodesAdded > 0 || connectionsAdded > 0) {
            overallImpact = ChangeImpact.MINOR;
        } else if (uiChanged || metadataChanged) {
            overallImpact = ChangeImpact.PATCH;
        } else {
            overallImpact = ChangeImpact.NONE;
        }
    }

    /**
     * Get human-readable summary
     */
    public String getSummaryText() {
        if (totalChanges == 0) {
            return "No changes detected";
        }

        List<String> parts = new ArrayList<>();

        if (nodesAdded > 0 || nodesDeleted > 0 || nodesModified > 0) {
            parts.add(String.format("%d nodes (%d added, %d deleted, %d modified)",
                    nodesAdded + nodesDeleted + nodesModified, nodesAdded, nodesDeleted, nodesModified));
        }

        if (connectionsAdded > 0 || connectionsDeleted > 0 || connectionsModified > 0) {
            parts.add(String.format("%d connections (%d added, %d deleted, %d modified)",
                    connectionsAdded + connectionsDeleted + connectionsModified,
                    connectionsAdded, connectionsDeleted, connectionsModified));
        }

        if (uiChanged) {
            parts.add("UI layout changed");
        }

        if (runtimeChanged) {
            parts.add("Runtime config changed");
        }

        if (metadataChanged) {
            parts.add("Metadata changed");
        }

        return String.format("Total: %d changes (%s) - Impact: %s",
                totalChanges, String.join(", ", parts), overallImpact);
    }

    @Override
    public String toString() {
        return getSummaryText();
    }
}