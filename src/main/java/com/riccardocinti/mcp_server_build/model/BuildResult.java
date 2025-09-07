package com.riccardocinti.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.model.enums.BuildStatus;
import com.riccardocinti.mcp_server_build.model.enums.ErrorType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class BuildResult {

    private boolean success;
    private BuildStatus status;
    private ErrorType errorType;
    private String message;
    private String projectPath;
    private BuilderConfiguration buildConfiguration;
    private ArtifactInfo artifactInfo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long buildDuration;
    private List<String> suggestions;

    // Default constructor
    public BuildResult() {
    }

    // Full constructor
    public BuildResult(boolean success, BuildStatus status, ErrorType errorType, String message,
                       String projectPath, BuilderConfiguration buildConfiguration, ArtifactInfo artifactInfo,
                       LocalDateTime startTime, LocalDateTime endTime, long buildDuration, List<String> suggestions) {
        this.success = success;
        this.status = status;
        this.errorType = errorType;
        this.message = message;
        this.projectPath = projectPath;
        this.buildConfiguration = buildConfiguration;
        this.artifactInfo = artifactInfo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.buildDuration = buildDuration;
        this.suggestions = suggestions;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public BuildStatus getStatus() {
        return status;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public BuilderConfiguration getBuildConfiguration() {
        return buildConfiguration;
    }

    public ArtifactInfo getArtifactInfo() {
        return artifactInfo;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getBuildDuration() {
        return buildDuration;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setStatus(BuildStatus status) {
        this.status = status;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public void setBuildConfiguration(BuilderConfiguration buildConfiguration) {
        this.buildConfiguration = buildConfiguration;
    }

    public void setArtifactInfo(ArtifactInfo artifactInfo) {
        this.artifactInfo = artifactInfo;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setBuildDuration(long buildDuration) {
        this.buildDuration = buildDuration;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private BuildStatus status;
        private ErrorType errorType;
        private String message;
        private String projectPath;
        private BuilderConfiguration buildConfiguration;
        private ArtifactInfo artifactInfo;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long buildDuration;
        private List<String> suggestions;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder status(BuildStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorType(ErrorType errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder buildConfiguration(BuilderConfiguration buildConfiguration) {
            this.buildConfiguration = buildConfiguration;
            return this;
        }

        public Builder artifactInfo(ArtifactInfo artifactInfo) {
            this.artifactInfo = artifactInfo;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder buildDuration(long buildDuration) {
            this.buildDuration = buildDuration;
            return this;
        }

        public Builder suggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public BuildResult build() {
            return new BuildResult(success, status, errorType, message, projectPath,
                    buildConfiguration, artifactInfo, startTime, endTime,
                    buildDuration, suggestions);
        }
    }

    // Factory methods for common scenarios
    public static BuildResult success(String projectPath, BuilderConfiguration config, ArtifactInfo artifacts,
                                      long duration, String message) {
        return BuildResult.builder()
                .success(true)
                .status(BuildStatus.COMPLETED)
                .projectPath(projectPath)
                .buildConfiguration(config)
                .artifactInfo(artifacts)
                .buildDuration(duration)
                .message(message)
                .endTime(LocalDateTime.now())
                .build();
    }

    public static BuildResult failure(ErrorType errorType, String message, String projectPath,
                                      List<String> suggestions) {
        LocalDateTime now = LocalDateTime.now();
        return BuildResult.builder()
                .success(false)
                .status(BuildStatus.FAILED)
                .errorType(errorType)
                .message(message)
                .projectPath(projectPath)
                .suggestions(suggestions)
                .startTime(now)
                .endTime(now)
                .buildDuration(0L)
                .build();
    }

    // Convenience methods
    public boolean hasArtifacts() {
        return artifactInfo != null && artifactInfo.getArtifactCount() > 0;
    }

    public boolean hasMainArtifact() {
        return hasArtifacts() && artifactInfo.hasMainArtifact();
    }

    public String getFormattedDuration() {
        if (buildDuration < 1000) {
            return buildDuration + "ms";
        } else if (buildDuration < 60000) {
            return String.format("%.1fs", buildDuration / 1000.0);
        } else {
            long minutes = buildDuration / 60000;
            long seconds = (buildDuration % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public String getBriefSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(success ? "BUILD SUCCESS" : "BUILD FAILED");

        if (buildConfiguration != null) {
            summary.append(" - ").append(buildConfiguration.buildTool());
        }

        summary.append(" (").append(getFormattedDuration()).append(")");

        if (hasArtifacts()) {
            summary.append(" - ").append(artifactInfo.getArtifactCount()).append(" artifacts");
        }

        return summary.toString();
    }

    public boolean hasSuggestions() {
        return suggestions != null && !suggestions.isEmpty();
    }

    public int getSuggestionCount() {
        return suggestions != null ? suggestions.size() : 0;
    }

    // Docker integration helpers
    public String getDockerBaseImage() {
        return buildConfiguration != null && buildConfiguration.dockerHints() != null ?
                buildConfiguration.dockerHints().baseImage() : null;
    }

    public String getExposedPort() {
        return buildConfiguration != null && buildConfiguration.dockerHints() != null ?
                buildConfiguration.dockerHints().exposedPort() : null;
    }

    public String getHealthCheckPath() {
        return buildConfiguration != null && buildConfiguration.dockerHints() != null ?
                buildConfiguration.dockerHints().healthCheckPath() : null;
    }

    public String getStartCommand() {
        return buildConfiguration != null && buildConfiguration.dockerHints() != null ?
                buildConfiguration.dockerHints().startCommand() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildResult that = (BuildResult) o;
        return success == that.success &&
                buildDuration == that.buildDuration &&
                status == that.status &&
                errorType == that.errorType &&
                Objects.equals(message, that.message) &&
                Objects.equals(projectPath, that.projectPath) &&
                Objects.equals(buildConfiguration, that.buildConfiguration) &&
                Objects.equals(artifactInfo, that.artifactInfo) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(suggestions, that.suggestions);
    }

}