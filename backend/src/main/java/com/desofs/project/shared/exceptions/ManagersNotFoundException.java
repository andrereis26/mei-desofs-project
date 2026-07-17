package com.desofs.project.shared.exceptions;

import java.util.Set;
import java.util.UUID;

public class ManagersNotFoundException extends RuntimeException {

    private final Set<UUID> missingUserIds;

    public ManagersNotFoundException(Set<UUID> missingUserIds) {
        super("Managers not found: " + missingUserIds);
        this.missingUserIds = missingUserIds;
    }

    public Set<UUID> getMissingUserIds() {
        return missingUserIds;
    }
}
