package com.riccardocinti.mcp_server_build.model;

import java.util.List;

public record CompilationResult(boolean success,
                                List<String> compiledFiles,
                                List<String> errors,
                                List<String> warnings,
                                String output,
                                long duration) {
    
    public static CompilationResult.Builder builder() {
        return new CompilationResult.Builder();
    }

    public static final class Builder {
        boolean success;
        List<String> compiledFiles;
        List<String> errors;
        List<String> warnings;
        String output;
        long duration;

        public CompilationResult.Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public CompilationResult.Builder compiledFiles(List<String> compiledFiles) {
            this.compiledFiles = compiledFiles;
            return this;
        }

        public CompilationResult.Builder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public CompilationResult.Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public CompilationResult.Builder output(String output) {
            this.output = output;
            return this;
        }

        public CompilationResult.Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public CompilationResult build() {
            return new CompilationResult(
                    this.success,
                    this.compiledFiles,
                    this.errors,
                    this.warnings,
                    this.output,
                    this.duration
            );
        }
    }
}
