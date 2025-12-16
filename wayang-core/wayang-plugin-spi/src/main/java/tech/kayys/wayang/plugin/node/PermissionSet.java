package tech.kayys.wayang.plugin.node;

import java.util.Set;

/**
 * Permissions granted to a node during execution.
 * Supports future expansion into policy-based governance.
 */
public interface PermissionSet {

    /**
     * Checks if the node has a given permission.
     *
     * @param permission name of the permission, e.g. "http:call", "db:write"
     */
    boolean has(String permission);

    /**
     * @return all granted permissions (read-only)
     */
    Set<String> getAll();
}
