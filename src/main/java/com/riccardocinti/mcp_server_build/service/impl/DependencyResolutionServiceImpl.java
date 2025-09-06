package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.config.BuilderServerConfig;
import com.riccardocinti.mcp_server_build.exceptions.DependencyResolutionException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.DependencyResult;
import com.riccardocinti.mcp_server_build.model.ProcessResult;
import com.riccardocinti.mcp_server_build.service.interfaces.DependencyResolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DependencyResolutionServiceImpl implements DependencyResolutionService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyResolutionServiceImpl.class);

    private static final Pattern MAVEN_DEPENDENCY_PATTERN = Pattern.compile("\\[INFO\\]\\s+([^:]+):([^:]+):([^:]+):([^:]+)");

    // Error patterns for different build tools
    private static final Pattern MAVEN_ERROR_PATTERN = Pattern.compile("\\[ERROR\\](.+)");

    private final BuilderServerConfig builderServerConfig;

    public DependencyResolutionServiceImpl(BuilderServerConfig builderServerConfig) {
        this.builderServerConfig = builderServerConfig;
    }

    @Override
    public DependencyResult resolveDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv)
            throws DependencyResolutionException {

        logger.debug("Starting dependency resolution for {} project", builderConfiguration.buildTool());
        Instant startTime = Instant.now();

        try {
            return switch (builderConfiguration.buildTool()) {
                case MAVEN -> resolveMavenDependencies(builderConfiguration, buildEnv);
                case GRADLE, GRADLE_KOTLIN -> resolveGradleDependencies(builderConfiguration, buildEnv);
                case NPM -> resolveNpmDependencies(builderConfiguration, buildEnv);
            };

        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            logger.error("Dependency resolution failed after {}ms", duration, e);
            throw new DependencyResolutionException("Failed to resolve dependencies for " + builderConfiguration.buildTool(), e);
        }
    }

    private DependencyResult resolveMavenDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv)
            throws DependencyResolutionException, IOException, InterruptedException, ExecutionException, TimeoutException {

        logger.debug("Resolving Maven dependencies");
        Instant startTime = Instant.now();

        List<String> command = buildMavenDependencyCommand(buildEnv);

        ProcessResult processResult = executeCommand(command, buildEnv);

        List<String> resolvedDependencies = parseMavenDependencies(processResult.output());
        List<String> failedDependencies = parseMavenFailures(processResult.output());

        long duration = Duration.between(startTime, Instant.now()).toMillis();
        boolean success = processResult.exitCode() == 0 && failedDependencies.isEmpty();

        DependencyResult result = DependencyResult.builder()
                .success(success)
                .resolvedDependencies(resolvedDependencies)
                .failedDependencies(failedDependencies)
                .output(processResult.output())
                .duration(duration)
                .build();

        logger.info("Maven dependency resolution completed - Success: {}, Resolved: {}, Failed: {}, Duration: {}ms",
                success, resolvedDependencies.size(), failedDependencies.size(), duration);

        if (!success) {
            throw new DependencyResolutionException("Maven dependency resolution failed: " +
                    String.join(", ", failedDependencies));
        }

        return result;
    }

    private DependencyResult resolveNpmDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) {
        throw new UnsupportedOperationException();
    }

    private DependencyResult resolveGradleDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) {
        throw new UnsupportedOperationException();
    }

    private List<String> buildMavenDependencyCommand(BuildEnvironment buildEnv) {
        List<String> command = new ArrayList<>();

        // Use Maven from MAVEN_HOME if available, otherwise assume it's in PATH
        String mavenHome = buildEnv.buildToolHome();
        if (mavenHome != null) {
            String mvnCmd = isWindows() ? "mvn.cmd" : "mvn";
            command.add(Paths.get(mavenHome, "bin", mvnCmd).toString());
        } else {
            command.add(isWindows() ? "mvn.cmd" : "mvn");
        }

        // Add Maven dependency resolution goals
        command.add("dependency:resolve");
        command.add("dependency:resolve-sources");

        // Add standard Maven flags
        command.add("--batch-mode");
        command.add("--no-transfer-progress");
        command.add("--quiet");

        logger.debug("Built Maven dependency command: {}", String.join(" ", command));
        return command;
    }

    private ProcessResult executeCommand(List<String> command, BuildEnvironment buildEnv)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new java.io.File(buildEnv.workingDirectory()));
        processBuilder.environment().putAll(buildEnv.environmentVariables());
        processBuilder.redirectErrorStream(true);

        logger.debug("Executing command in directory {}: {}",
                buildEnv.workingDirectory(), String.join(" ", command));

        Process process = processBuilder.start();

        // Create async task to read process output
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log important lines for debugging
                    if (line.contains("ERROR") || line.contains("FAILURE") || line.contains("npm ERR")) {
                        logger.debug("Process output: {}", line);
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading process output", e);
            }
            return output.toString();
        });

        // Wait for process with timeout
        long timeoutMs = builderServerConfig.getTimeout();
        boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new InterruptedException("Process timed out after " + timeoutMs + "ms");
        }

        String output = outputFuture.get(5, TimeUnit.SECONDS); // Give some time for output reading
        int exitCode = process.exitValue();

        logger.debug("Process completed with exit code: {}", exitCode);
        return new ProcessResult(exitCode, output);
    }

    private List<String> parseMavenDependencies(String output) {
        List<String> dependencies = new ArrayList<>();

        // Parse Maven dependency tree output
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains(":")) {
                Matcher matcher = MAVEN_DEPENDENCY_PATTERN.matcher(line);
                if (matcher.find()) {
                    String dependency = matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(4);
                    dependencies.add(dependency);
                }
            }
        }

        logger.debug("Parsed {} Maven dependencies", dependencies.size());
        return dependencies;
    }

    private List<String> parseMavenFailures(String output) {
        List<String> failures = new ArrayList<>();

        String[] lines = output.split("\n");
        for (String line : lines) {
            Matcher matcher = MAVEN_ERROR_PATTERN.matcher(line);
            if (matcher.find()) {
                String error = matcher.group(1).trim();
                if (error.contains("Could not resolve") || error.contains("Failed to")) {
                    failures.add(error);
                }
            }
        }

        return failures;
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

}
