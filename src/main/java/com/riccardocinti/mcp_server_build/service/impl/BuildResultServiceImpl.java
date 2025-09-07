package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.model.*;
import com.riccardocinti.mcp_server_build.model.enums.BuildStatus;
import com.riccardocinti.mcp_server_build.model.enums.ErrorType;
import com.riccardocinti.mcp_server_build.service.interfaces.BuildResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BuildResultServiceImpl implements BuildResultService {
    private static final Logger logger = LoggerFactory.getLogger(BuildResultServiceImpl.class);

    @Override
    public BuildResult compileBuildResult(ProjectInfo projectInfo,
                                          BuilderConfiguration buildConfig,
                                          BuildEnvironment buildEnv,
                                          DependencyResult depResult,
                                          CompilationResult compResult,
                                          ArtifactInfo artifactInfo) {

        logger.debug("Compiling build result for {} project: {}",
                projectInfo.buildTool(), projectInfo.projectName());

        LocalDateTime endTime = LocalDateTime.now();

        // Calculate total build duration from individual phases
        long totalDuration = calculateTotalDuration(depResult, compResult);

        // Determine overall build status
        BuildStatus finalStatus = determineFinalStatus(depResult, compResult, artifactInfo);
        boolean success = finalStatus == BuildStatus.COMPLETED;

        // Compile comprehensive build result
        BuildResult.Builder resultBuilder = BuildResult.builder()
                .success(success)
                .status(finalStatus)
                .projectPath(projectInfo.projectPath())
                .buildConfiguration(buildConfig)
                .artifactInfo(artifactInfo)
                .endTime(endTime)
                .buildDuration(totalDuration);

        // Add success-specific information
        if (success) {
            resultBuilder.message(generateSuccessMessage(projectInfo, buildConfig, artifactInfo));
        } else {
            // Build failed, but we still want to provide partial results
            resultBuilder.message(generatePartialResultMessage(depResult, compResult));
            resultBuilder.suggestions(generateFailureRecoveryTips(buildConfig, depResult, compResult));
        }

        BuildResult result = resultBuilder.build();

        // Log comprehensive build summary
        logBuildSummary(result, projectInfo, depResult, compResult, artifactInfo);

        return result;
    }

    @Override
    public BuildResult createFailureResult(ErrorType errorType, Exception exception, String projectPath) {
        logger.error("Creating failure result - ErrorType: {}, Message: {}", errorType, exception.getMessage());

        String message = exception.getMessage();
        if (exception.getCause() != null) {
            message += " - Caused by: " + exception.getCause().getMessage();
        }

        LocalDateTime now = LocalDateTime.now();

        BuildResult result = BuildResult.builder()
                .success(false)
                .status(BuildStatus.FAILED)
                .errorType(errorType)
                .message(message)
                .projectPath(projectPath)
                .startTime(now)
                .endTime(now)
                .buildDuration(0L)
                .suggestions(generateErrorTypeSpecificSuggestions(errorType, exception.getMessage()))
                .build();

        logger.info("Build failure result created for error type: {}", errorType);
        return result;
    }

    private long calculateTotalDuration(DependencyResult depResult, CompilationResult compResult) {
        long dependencyDuration = depResult != null ? depResult.duration() : 0L;
        long compilationDuration = compResult != null ? compResult.duration() : 0L;

        // Add estimated overhead for project discovery, environment setup, and artifact discovery
        long estimatedOverhead = 2000L; // 2 seconds estimated overhead

        return dependencyDuration + compilationDuration + estimatedOverhead;
    }

    private BuildStatus determineFinalStatus(DependencyResult depResult, CompilationResult compResult, ArtifactInfo artifactInfo) {
        // Check dependency resolution status
        if (depResult == null || !depResult.success()) {
            return BuildStatus.FAILED;
        }

        // Check compilation status
        if (compResult == null || !compResult.success()) {
            return BuildStatus.FAILED;
        }

        // Check if we have any artifacts (some builds might succeed but produce no artifacts)
        if (artifactInfo == null || artifactInfo.getArtifactCount() == 0) {
            logger.warn("Build completed but no artifacts were found");
            // This might still be considered successful for some project types
            return BuildStatus.COMPLETED;
        }

        return BuildStatus.COMPLETED;
    }

    private String generateSuccessMessage(ProjectInfo projectInfo, BuilderConfiguration buildConfig, ArtifactInfo artifactInfo) {
        StringBuilder message = new StringBuilder();

        message.append("Build completed successfully for ")
                .append(buildConfig.buildTool())
                .append(" project '")
                .append(projectInfo.projectName())
                .append("'");

        if (projectInfo.projectVersion() != null && !projectInfo.projectVersion().equals("unknown")) {
            message.append(" version ").append(projectInfo.projectVersion());
        }

        message.append(".");

        // Add artifact information
        if (artifactInfo != null && artifactInfo.getArtifactCount() > 0) {
            message.append(" Generated ")
                    .append(artifactInfo.getArtifactCount())
                    .append(" artifact")
                    .append(artifactInfo.getArtifactCount() > 1 ? "s" : "")
                    .append(" totaling ")
                    .append(artifactInfo.getFormattedSize());

            if (artifactInfo.hasMainArtifact()) {
                String mainArtifactName = java.nio.file.Paths.get(artifactInfo.mainArtifactPath())
                        .getFileName().toString();
                message.append(". Main executable: ").append(mainArtifactName);
            }
        }

        // Add framework-specific information
        if (buildConfig.port() != null) {
            message.append(" Ready to run on port ").append(buildConfig.port());
        }

        return message.toString();
    }

    private String generatePartialResultMessage(DependencyResult depResult, CompilationResult compResult) {
        StringBuilder message = new StringBuilder("Build failed");

        if (depResult != null && !depResult.success()) {
            message.append(" during dependency resolution");
            if (depResult.failedDependencies() != null && !depResult.failedDependencies().isEmpty()) {
                message.append(" (").append(depResult.failedDependencies().size()).append(" failed dependencies)");
            }
        } else if (compResult != null && !compResult.success()) {
            message.append(" during compilation");
            if (compResult.errors().size() > 0) {
                message.append(" (").append(compResult.errors().size()).append(" compilation errors)");
            }
        }

        message.append(".");

        return message.toString();
    }

    private List<String> generateFailureRecoveryTips(BuilderConfiguration buildConfig, DependencyResult depResult, CompilationResult compResult) {
        List<String> suggestions = new ArrayList<>();

        // Dependency-related suggestions
        if (depResult != null && !depResult.success()) {
            suggestions.addAll(generateDependencyFailureSuggestions(buildConfig, depResult));
        }

        // Compilation-related suggestions
        if (compResult != null && !compResult.success()) {
            suggestions.addAll(generateCompilationFailureSuggestions(buildConfig, compResult));
        }

        // General suggestions if no specific ones were generated
        if (suggestions.isEmpty()) {
            suggestions.addAll(generateGeneralBuildSuggestions(buildConfig));
        }

        return suggestions;
    }

    private List<String> generateDependencyFailureSuggestions(BuilderConfiguration buildConfig, DependencyResult depResult) {
        List<String> suggestions = new ArrayList<>();

        switch (buildConfig.buildTool()) {
            case MAVEN -> {
                suggestions.add("Check internet connectivity and Maven repository access");
                suggestions.add("Verify pom.xml dependency declarations are correct");
                suggestions.add("Try running 'mvn dependency:purge-local-repository' to clear corrupted cache");
                suggestions.add("Check if corporate proxy/firewall is blocking Maven Central access");
            }
            case GRADLE, GRADLE_KOTLIN -> {
                suggestions.add("Check internet connectivity and Gradle repository access");
                suggestions.add("Verify build.gradle dependency declarations are correct");
                suggestions.add("Try running 'gradle --refresh-dependencies' to force dependency refresh");
                suggestions.add("Check Gradle daemon status with 'gradle --status'");
            }
            case NPM -> {
                suggestions.add("Check internet connectivity and NPM registry access");
                suggestions.add("Verify package.json dependency versions are correct");
                suggestions.add("Try clearing NPM cache with 'npm cache clean --force'");
                suggestions.add("Consider using 'npm ci' instead of 'npm install' for clean installs");
            }
        }

        return suggestions;
    }

    private List<String> generateCompilationFailureSuggestions(BuilderConfiguration buildConfig, CompilationResult compResult) {
        List<String> suggestions = new ArrayList<>();

        // Analyze compilation errors for common patterns
        if (compResult.errors() != null) {
            boolean hasJavaVersionIssues = compResult.errors().stream()
                    .anyMatch(error -> error.contains("unsupported") &&
                            (error.contains("class file version") || error.contains("source version")));

            boolean hasMemoryIssues = compResult.errors().stream()
                    .anyMatch(error -> error.contains("OutOfMemoryError") || error.contains("Java heap space"));

            boolean hasMissingClasses = compResult.errors().stream()
                    .anyMatch(error -> error.contains("cannot find symbol") || error.contains("package does not exist"));

            if (hasJavaVersionIssues) {
                suggestions.add("Java version mismatch detected. Ensure JAVA_HOME points to Java " +
                        buildConfig.javaVersion());
                suggestions.add("Check project's Java source and target compatibility settings");
            }

            if (hasMemoryIssues) {
                suggestions.add("Increase JVM heap memory with -Xmx flag in build tool options");
                suggestions.add("Consider enabling parallel compilation to reduce memory pressure");
            }

            if (hasMissingClasses) {
                suggestions.add("Missing dependencies detected. Run dependency resolution again");
                suggestions.add("Check if all required dependencies are declared in build file");
            }
        }

        // General compilation suggestions
        suggestions.add("Review compilation errors in the build output for specific issues");
        suggestions.add("Ensure all source files have correct syntax and imports");

        return suggestions;
    }

    private List<String> generateGeneralBuildSuggestions(BuilderConfiguration buildConfig) {
        List<String> suggestions = new ArrayList<>();

        suggestions.add("Check build tool installation and PATH configuration");
        suggestions.add("Verify project structure follows " + buildConfig.buildTool() + " conventions");
        suggestions.add("Ensure all required dependencies and plugins are available");
        suggestions.add("Review build configuration file for syntax errors");

        return suggestions;
    }

    private List<String> generateErrorTypeSpecificSuggestions(ErrorType errorType, String message) {
        List<String> suggestions = new ArrayList<>();

        switch (errorType) {
            case PROJECT_DISCOVERY_FAILED -> {
                suggestions.add("Ensure the project path exists and is accessible");
                suggestions.add("Check file system permissions for the project directory");
                suggestions.add("Verify the project contains a valid build configuration file (pom.xml, build.gradle, package.json)");
            }
            case BUILD_TOOL_DETECTION_FAILED -> {
                suggestions.add("Ensure a supported build tool configuration file is present");
                suggestions.add("Supported files: pom.xml (Maven), build.gradle/.kts (Gradle), package.json (NPM)");
                suggestions.add("Check that the configuration file is not corrupted or empty");
            }
            case BUILD_ENVIRONMENT_FAILED -> {
                suggestions.add("Install required build tools (Maven, Gradle, Node.js/NPM)");
                suggestions.add("Ensure build tools are available in PATH or set appropriate _HOME environment variables");
                suggestions.add("Verify Java installation and JAVA_HOME environment variable");
            }
            case DEPENDENCY_RESOLUTION_FAILED -> {
                suggestions.add("Check internet connectivity and repository accessibility");
                suggestions.add("Verify dependency declarations in build configuration");
                suggestions.add("Clear build tool cache and retry");
            }
            case COMPILATION_FAILED -> {
                suggestions.add("Review source code for syntax errors");
                suggestions.add("Ensure all dependencies are properly resolved");
                suggestions.add("Check Java version compatibility");
            }
            case ARTIFACT_DISCOVERY_FAILED -> {
                suggestions.add("Check if compilation completed successfully");
                suggestions.add("Verify build output directories exist and are accessible");
                suggestions.add("Review build configuration for custom output paths");
            }
            case UNEXPECTED_ERROR -> {
                suggestions.add("Review detailed error message for specific issues");
                suggestions.add("Check system resources (disk space, memory)");
                suggestions.add("Retry the build operation");
            }
        }

        return suggestions;
    }

    private void logBuildSummary(BuildResult result, ProjectInfo projectInfo, DependencyResult depResult,
                                 CompilationResult compResult, ArtifactInfo artifactInfo) {

        StringBuilder summary = new StringBuilder();
        summary.append("\n").append("=".repeat(80)).append("\n");
        summary.append("BUILD SUMMARY\n");
        summary.append("=".repeat(80)).append("\n");

        // Project information
        summary.append("Project: ").append(projectInfo.projectName());
        if (projectInfo.projectVersion() != null && !projectInfo.projectVersion().equals("unknown")) {
            summary.append(" v").append(projectInfo.projectVersion());
        }
        summary.append("\n");
        summary.append("Build Tool: ").append(projectInfo.buildTool()).append("\n");
        summary.append("Java Version: ").append(result.getBuildConfiguration().javaVersion()).append("\n");

        // Build phases summary
        summary.append("\n--- Build Phases ---\n");

        if (depResult != null) {
            summary.append("Dependencies: ")
                    .append(depResult.success() ? "SUCCESS" : "FAILED")
                    .append(" (").append(depResult.duration()).append("ms, ")
                    .append(depResult.resolvedDependencies().size()).append(" resolved");
            if (depResult.failedDependencies().size() > 0) {
                summary.append(", ").append(depResult.failedDependencies().size()).append(" failed");
            }
            summary.append(")\n");
        }

        if (compResult != null) {
            summary.append("Compilation: ")
                    .append(compResult.success() ? "SUCCESS" : "FAILED")
                    .append(" (").append(compResult.duration()).append("ms, ")
                    .append(compResult.compiledFiles().size()).append(" files");
            if (compResult.errors().size() > 0) {
                summary.append(", ").append(compResult.errors().size()).append(" errors");
            }
            if (compResult.warnings().size() > 0) {
                summary.append(", ").append(compResult.warnings().size()).append(" warnings");
            }
            summary.append(")\n");
        }

        // Artifacts summary
        if (artifactInfo != null && artifactInfo.getArtifactCount() > 0) {
            summary.append("\n--- Artifacts ---\n");
            summary.append("Total: ").append(artifactInfo.getArtifactCount())
                    .append(" artifacts (").append(artifactInfo.getFormattedSize()).append(")\n");

            if (artifactInfo.hasMainArtifact()) {
                String mainArtifactName = java.nio.file.Paths.get(artifactInfo.mainArtifactPath())
                        .getFileName().toString();
                summary.append("Main: ").append(mainArtifactName).append("\n");
            }
        }

        // Overall result
        summary.append("\n--- Result ---\n");
        summary.append("Status: ").append(result.isSuccess() ? "SUCCESS" : "FAILED").append("\n");
        summary.append("Duration: ").append(result.getBuildDuration()).append("ms\n");

        if (result.getBuildConfiguration().port() != null) {
            summary.append("Port: ").append(result.getBuildConfiguration().port()).append("\n");
        }

        summary.append("=".repeat(80));

        if (result.isSuccess()) {
            logger.info(summary.toString());
        } else {
            logger.error(summary.toString());
            if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()) {
                logger.info("Recovery suggestions:");
                result.getSuggestions().forEach(suggestion -> logger.info("  • {}", suggestion));
            }
        }
    }
}
