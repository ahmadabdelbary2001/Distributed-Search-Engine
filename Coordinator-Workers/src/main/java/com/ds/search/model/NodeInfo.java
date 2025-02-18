package com.ds.search.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class NodeInfo {
    private boolean isCoordinator;

    public NodeInfo(boolean isCoordinator) {
        this.isCoordinator = isCoordinator;
    }
}
