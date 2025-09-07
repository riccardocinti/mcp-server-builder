package com.riccardocinti.mcp_server_build.exceptions;

public class ArtifactDiscoveryException extends McpServerBuilderException{

    public ArtifactDiscoveryException(String message) {
        super(message);
    }

    public ArtifactDiscoveryException(String message, Throwable e) {
        super(message, e);
    }

}
