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
        public static ArtifactBuilder builder() {
            return new ArtifactBuilder();
        }

        public static class ArtifactBuilder {
            private String path;
            private String type;
            private long size;
            private String checksum;

            public ArtifactBuilder path(String path) {
                this.path = path;
                return this;
            }

            public ArtifactBuilder type(String type) {
                this.type = type;
                return this;
            }

            public ArtifactBuilder size(long size) {
                this.size = size;
                return this;
            }

            public ArtifactBuilder checksum(String checksum) {
                this.checksum = checksum;
                return this;
            }

            public Artifact build() {
                return new Artifact(path, type, size, checksum);
            }
        }

        // Convenience methods
        public String getFileName() {
            return path != null ? java.nio.file.Paths.get(path).getFileName().toString() : null;
        }

        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else {
                return String.format("%.1f MB", size / (1024.0 * 1024.0));
            }
        }

    }

    public static ArtifactInfo.Builder builder() {
        return new ArtifactInfo.Builder();
    }

    public int getArtifactCount() {
        return artifacts != null ? artifacts.size() : 0;
    }

    public String getFormattedSize() {
        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.1f KB", totalSize / 1024.0);
        } else {
            return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
        }
    }

    public boolean hasMainArtifact() {
        return mainArtifactPath != null && !mainArtifactPath.trim().isEmpty();
    }

    public static class Builder {
        List<Artifact> artifacts;
        String mainArtifactPath;
        long totalSize;

        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public Builder mainArtifactPath(String mainArtifactPath) {
            this.mainArtifactPath = mainArtifactPath;
            return this;
        }

        public Builder totalSize(long totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public ArtifactInfo build() {
            return new ArtifactInfo(
                    this.artifacts,
                    this.mainArtifactPath,
                    this.totalSize
            );
        }
    }
}
