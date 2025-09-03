package com.riccardocinti.mcp_server_build.exceptions;

public class McpServerBuilderException extends RuntimeException {
    public McpServerBuilderException(String message) {
        super(message);
    }

    public McpServerBuilderException(String message, Throwable e) {
        super(message, e);
    }
}
