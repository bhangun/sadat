package tech.kayys.wayang.schema.governance;

import java.util.List;

public class AccessPolicy {
    private List<String> rolesAllowed;
    private List<String> principals;

    public List<String> getRolesAllowed() {
        return rolesAllowed;
    }

    public void setRolesAllowed(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    public List<String> getPrincipals() {
        return principals;
    }

    public void setPrincipals(List<String> principals) {
        this.principals = principals;
    }
}
