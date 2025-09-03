package com.riccardocinti.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.model.enums.BuildTool;

import java.util.List;

public record ProjectInfo(String projectPath,
                          String projectName,
                          String projectVersion,
                          BuildTool buildTool,
                          String buildFilePath,
                          List<String> additionalFiles) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        String projectPath;
        String projectName;
        String projectVersion;
        BuildTool buildTool;
        String buildFilePath;
        List<String> additionalFiles;

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder projectVersion(String projectVersion) {
            this.projectVersion = projectVersion;
            return this;
        }

        public Builder buildTool(BuildTool buildTool) {
            this.buildTool = buildTool;
            return this;
        }

        public Builder buildFilePath(String buildFilePath) {
            this.buildFilePath = buildFilePath;
            return this;
        }

        public Builder additionalFiles(List<String> additionalFiles) {
            this.additionalFiles = additionalFiles;
            return this;
        }

        public ProjectInfo build() {
            return new ProjectInfo(projectPath, projectName, projectVersion,
                    buildTool, buildFilePath, additionalFiles);
        }
    }
}
