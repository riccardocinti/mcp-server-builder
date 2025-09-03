package com.riccardocinti.mcp_server_build.model;

public record DockerHints(String baseImage,
                          String exposedPort,
                          String healthCheckPath,
                          String workdir,
                          String startCommand) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        String baseImage;
        String exposedPort;
        String healthCheckPath;
        String workDir;
        String startCommand;

        public Builder baseImage(String baseImage) {
            this.baseImage = baseImage;
            return this;
        }

        public Builder exposedPort(String exposedPort) {
            this.exposedPort = exposedPort;
            return this;
        }

        public Builder healthCheckPath(String healthCheckPath) {
            this.healthCheckPath = healthCheckPath;
            return this;
        }

        public Builder workDir(String workDir) {
            this.workDir = workDir;
            return this;
        }

        public Builder startCommand(String startCommand) {
            this.startCommand = startCommand;
            return this;
        }

        public DockerHints build() {
            return new DockerHints(baseImage, exposedPort, healthCheckPath, workDir, startCommand);
        }
    }
}
