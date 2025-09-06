package com.riccardocinti.mcp_server_build.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mcp.builder")
public class BuilderServerConfig {

    private long timeout;
    private int maxConnections;
    private String tempDirectory;
    private boolean preserveArtifacts;

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public boolean isPreserveArtifacts() {
        return preserveArtifacts;
    }

    public void setPreserveArtifacts(boolean preserveArtifacts) {
        this.preserveArtifacts = preserveArtifacts;
    }
}
