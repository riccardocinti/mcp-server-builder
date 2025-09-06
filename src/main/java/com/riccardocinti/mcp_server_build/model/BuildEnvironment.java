package com.riccardocinti.mcp_server_build.model;

import java.util.Map;

public record BuildEnvironment(Map<String, String> environmentVariables,
                               String workingDirectory,
                               String javaHome,
                               String buildToolHome,
                               String tempDirectory) {

    public static BuildEnvironment.Builder builder() {
        return new BuildEnvironment.Builder();
    }

    public static final class Builder {

        Map<String, String> environmentVariables;
        String workingDirectory;
        String javaHome;
        String buildToolHome;
        String tempDirectory;

        public Builder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder buildToolHome(String buildToolHome) {
            this.buildToolHome = buildToolHome;
            return this;
        }

        public Builder tempDirectory(String tempDirectory) {
            this.tempDirectory = tempDirectory;
            return this;
        }

        public BuildEnvironment build() {
            return new BuildEnvironment(
                    this.environmentVariables,
                    this.workingDirectory,
                    this.javaHome,
                    this.buildToolHome,
                    this.tempDirectory);
        }
    }
}
