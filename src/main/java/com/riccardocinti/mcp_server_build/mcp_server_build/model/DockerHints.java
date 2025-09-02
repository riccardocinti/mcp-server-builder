package com.riccardocinti.mcp_server_build.mcp_server_build.model;

public record DockerHints(String baseImage,
                          String exposedPort,
                          String healthCheckPath,
                          String workdir,
                          String startCommand) {
}
