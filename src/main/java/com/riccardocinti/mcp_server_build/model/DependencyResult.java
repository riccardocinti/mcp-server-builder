package com.riccardocinti.mcp_server_build.model;

import java.util.List;

public record DependencyResult(boolean success,
                               List<String> resolvedDependencies,
                               List<String> failedDependencies,
                               String output,
                               long duration) {

    public static DependencyResult.Builder builder() {
        return new DependencyResult.Builder();
    }

    public static final class Builder {
        boolean success;
        List<String> resolvedDependencies;
        List<String> failedDependencies;
        String output;
        long duration;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder resolvedDependencies(List<String> resolvedDependencies) {
            this.resolvedDependencies = resolvedDependencies;
            return this;
        }

        public Builder failedDependencies(List<String> failedDependencies) {
            this.failedDependencies = failedDependencies;
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder duration(long duration) {
            this.duration = duration;
            return this;
        }

        public DependencyResult build() {
            return new DependencyResult(
                    this.success,
                    this.resolvedDependencies,
                    this.failedDependencies,
                    this.output,
                    this.duration
            );
        }
    }
}
