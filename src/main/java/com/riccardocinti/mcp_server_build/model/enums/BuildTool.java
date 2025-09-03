package com.riccardocinti.mcp_server_build.model.enums;

public enum BuildTool {
    MAVEN("maven", "pom.xml"),
    GRADLE("gradle", "build.gradle"),
    GRADLE_KOTLIN("gradle", "build.gradle.kts"),
    NPM("npm", "package.json");

    private final String toolName;
    private final String configFile;

    BuildTool(String toolName, String configFile) {
        this.toolName = toolName;
        this.configFile = configFile;
    }

    // TODO: Implement getters and utility methods
}
