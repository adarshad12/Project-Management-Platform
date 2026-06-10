package com.dealshare.projectmanagement.security;

public enum ProjectRole {
    ADMIN("admin", 4),
    PROJECT_LEAD("project_lead", 3),
    MEMBER("member", 2),
    VIEWER("viewer", 1);

    private final String value;
    private final int rank;

    ProjectRole(String value, int rank) {
        this.value = value;
        this.rank = rank;
    }

    public String value() {
        return value;
    }

    public boolean atLeast(ProjectRole required) {
        return rank >= required.rank;
    }

    public static ProjectRole fromValue(String value) {
        for (ProjectRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
