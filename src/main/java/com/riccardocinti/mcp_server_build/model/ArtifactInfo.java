package com.riccardocinti.mcp_server_build.model;

import java.util.List;

public record ArtifactInfo(
        List<Artifact> artifacts,
        String mainArtifactPath,
        long totalSize) {

    public record Artifact(
            String path,
            String type,
            long size,
            String checksum) {
    }
}
