package com.riccardocinti.mcp_server_build.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BuilderServerConfig {

    @Value("${mcp.builder.timeout:300000}")
    private long timeout;

    @Value("${mcp.builder.max-connections:3}")
    private int maxConnections;

    @Value("${mcp.builder.temp-directory:#{systemProperties['java.io.tmpdir']}/mcp-builds}")
    private String tempDirectory;

    @Value("${mcp.builder.preserve-artifacts:true}")
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
