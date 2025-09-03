package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.mcp_server_build.model.enums.BuildTool;

import java.util.List;

public record ProjectInfo(String projectPath,
                          String projectName,
                          String projectVersion,
                          BuildTool buildTool,
                          String buildFilePath,
                          List<String> additionalFiles) {

    public static ProjectInfoBuilder builder() {
        return new ProjectInfoBuilder();
    }

    public static final class ProjectInfoBuilder {
        String projectPath;
        String projectName;
        String projectVersion;
        BuildTool buildTool;
        String buildFilePath;
        List<String> additionalFiles;

        public ProjectInfoBuilder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public ProjectInfoBuilder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public ProjectInfoBuilder projectVersion(String projectVersion) {
            this.projectVersion = projectVersion;
            return this;
        }

        public ProjectInfoBuilder buildTool(BuildTool buildTool) {
            this.buildTool = buildTool;
            return this;
        }

        public ProjectInfoBuilder buildFilePath(String buildFilePath) {
            this.buildFilePath = buildFilePath;
            return this;
        }

        public ProjectInfoBuilder additionalFiles(List<String> additionalFiles) {
            this.additionalFiles = additionalFiles;
            return this;
        }

        public ProjectInfo build() {
            return new ProjectInfo(projectPath, projectName, projectVersion,
                    buildTool, buildFilePath, additionalFiles);
        }
    }
}
