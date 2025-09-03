package com.riccardocinti.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.model.enums.BuildTool;

import java.util.List;
import java.util.Map;

public record BuilderConfiguration(
        BuildTool buildTool,
        String projectPath,
        String javaVersion,
        String mainClass,
        String port,
        List<String> buildCommands,
        Map<String, String> properties,
        DockerHints dockerHints
) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        BuildTool buildTool;
        String projectPath;
        String javaVersion;
        String mainClass;
        String port;
        List<String> buildCommands;
        Map<String, String> properties;
        DockerHints dockerHints;

        public Builder buildTool(BuildTool buildTool) {
            this.buildTool = buildTool;
            return this;
        }

        public Builder projectPath(String projectPath) {
            this.projectPath = projectPath;
            return this;
        }

        public Builder javaVersion(String javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public Builder port(String port) {
            this.port = port;
            return this;
        }

        public Builder buildCommands(List<String> buildCommands) {
            this.buildCommands = buildCommands;
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder dockerHints(DockerHints dockerHints) {
            this.dockerHints = dockerHints;
            return this;
        }

        public BuilderConfiguration build() {
            return new BuilderConfiguration(buildTool, projectPath, javaVersion, mainClass,
                    port, buildCommands, properties, dockerHints);
        }
    }
}
