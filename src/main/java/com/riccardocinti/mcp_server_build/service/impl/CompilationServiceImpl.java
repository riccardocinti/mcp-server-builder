package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.config.BuilderServerConfig;
import com.riccardocinti.mcp_server_build.exceptions.CompilationException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.CompilationResult;
import com.riccardocinti.mcp_server_build.model.ProcessResult;
import com.riccardocinti.mcp_server_build.service.interfaces.CompilationService;
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
public class CompilationServiceImpl implements CompilationService {

    private static final Logger logger = LoggerFactory.getLogger(CompilationServiceImpl.class);

    private static final Pattern MAVEN_SUCCESS_PATTERN = Pattern.compile("\\[INFO\\] BUILD SUCCESS");
    private static final Pattern MAVEN_ERROR_PATTERN = Pattern.compile("\\[ERROR\\](.+)");
    private static final Pattern MAVEN_WARNING_PATTERN = Pattern.compile("\\[WARNING\\](.+)");
    private static final Pattern MAVEN_COMPILED_PATTERN = Pattern.compile("\\[INFO\\] Compiling ([0-9]+) source files");

    private final BuilderServerConfig builderServerConfig;

    public CompilationServiceImpl(BuilderServerConfig builderServerConfig) {
        this.builderServerConfig = builderServerConfig;
    }

    @Override
    public CompilationResult compileAndPackage(BuilderConfiguration builderConfiguration,
                                               BuildEnvironment buildEnv) throws CompilationException {

        logger.info("Starting compilation and packaging for {} project", builderConfiguration.buildTool());
        Instant startTime = Instant.now();

        try {
            return switch (builderConfiguration.buildTool()) {
                case MAVEN -> compileMavenProject(builderConfiguration, buildEnv);
                case GRADLE, GRADLE_KOTLIN -> compileGradleProject(builderConfiguration, buildEnv);
                case NPM -> compileNpmProject(builderConfiguration, buildEnv);
            };
        } catch (Exception e) {
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            logger.error("Compilation failed after {}ms", duration, e);
            throw new CompilationException("Failed to compile " + builderConfiguration.buildTool() + " project", e);
        }
    }

    private CompilationResult compileMavenProject(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) throws IOException, ExecutionException, InterruptedException, TimeoutException {
        logger.debug("Compiling Maven project with commands: {}", builderConfiguration.buildCommands());
        Instant startTime = Instant.now();

        List<String> compiledFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        StringBuilder fullOutput = new StringBuilder();

        // Execute each build command sequentially
        for (String commandStr : builderConfiguration.buildCommands()) {
            logger.debug("Executing Maven command: {}", commandStr);

            List<String> command = buildMavenCommand(commandStr, buildEnv);
            ProcessResult processResult = executeCommand(command, buildEnv);

            fullOutput.append("=== Command: ").append(commandStr).append(" ===\n");
            fullOutput.append(processResult.output()).append("\n");

            // Parse Maven output for this command
            parseMavenOutput(processResult.output(), compiledFiles, errors, warnings);

            // Check if command failed
            if (processResult.exitCode() != 0) {
                long duration = Duration.between(startTime, Instant.now()).toMillis();

                CompilationResult result = CompilationResult.builder()
                        .success(false)
                        .compiledFiles(compiledFiles)
                        .errors(errors)
                        .warnings(warnings)
                        .output(fullOutput.toString())
                        .duration(duration)
                        .build();

                logger.error("Maven compilation failed at command: {}", commandStr);
                throw new CompilationException("Maven command failed: " + commandStr +
                        ". Errors: " + String.join(", ", errors));
            }
        }

        long duration = Duration.between(startTime, Instant.now()).toMillis();
        boolean success = errors.isEmpty();

        CompilationResult result = CompilationResult.builder()
                .success(success)
                .compiledFiles(compiledFiles)
                .errors(errors)
                .warnings(warnings)
                .output(fullOutput.toString())
                .duration(duration)
                .build();

        logger.info("Maven compilation completed - Success: {}, Files: {}, Errors: {}, Warnings: {}, Duration: {}ms",
                success, compiledFiles.size(), errors.size(), warnings.size(), duration);

        return result;
    }

    private CompilationResult compileNpmProject(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) {
        throw new UnsupportedOperationException();
    }

    private CompilationResult compileGradleProject(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) {
        throw new UnsupportedOperationException();
    }

    private List<String> buildMavenCommand(String commandStr, BuildEnvironment buildEnv) {
        List<String> command = new ArrayList<>();

        // Use Maven from MAVEN_HOME if available, otherwise assume it's in PATH
        String mavenHome = buildEnv.buildToolHome();
        if (mavenHome != null) {
            String mvnCmd = isWindows() ? "mvn.cmd" : "mvn";
            command.add(Paths.get(mavenHome, "bin", mvnCmd).toString());
        } else {
            command.add(isWindows() ? "mvn.cmd" : "mvn");
        }

        // Parse command string and add arguments
        String[] args = commandStr.split("\\s+");
        for (int i = 1; i < args.length; i++) { // Skip first element which is 'mvn'
            command.add(args[i]);
        }

        // Add standard Maven flags if not already present
        if (!commandStr.contains("--batch-mode")) {
            command.add("--batch-mode");
        }
        if (!commandStr.contains("--no-transfer-progress")) {
            command.add("--no-transfer-progress");
        }

        return command;
    }

    private ProcessResult executeCommand(List<String> command, BuildEnvironment buildEnv)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new java.io.File(buildEnv.workingDirectory()));
        processBuilder.environment().putAll(buildEnv.environmentVariables());
        processBuilder.redirectErrorStream(true);

        logger.debug("Executing compilation command in directory {}: {}",
                buildEnv.workingDirectory(), String.join(" ", command));

        Process process = processBuilder.start();

        // Create async task to read process output with real-time logging
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    // Log important lines for real-time feedback
                    if (line.contains("ERROR") || line.contains("FAILURE") || line.contains("BUILD SUCCESSFUL") ||
                            line.contains("BUILD SUCCESS") || line.contains("Compiling")) {
                        logger.info("Build output: {}", line);
                    } else if (line.contains("WARNING") || line.contains("WARN")) {
                        logger.debug("Build warning: {}", line);
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
            logger.warn("Process timed out after {}ms, terminating forcefully", timeoutMs);
            process.destroyForcibly();
            throw new InterruptedException("Compilation process timed out after " + timeoutMs + "ms");
        }

        String output = outputFuture.get(10, TimeUnit.SECONDS); // Give time for output reading
        int exitCode = process.exitValue();

        logger.debug("Compilation process completed with exit code: {}", exitCode);
        return new ProcessResult(exitCode, output);
    }

    private void parseMavenOutput(String output, List<String> compiledFiles, List<String> errors, List<String> warnings) {
        String[] lines = output.split("\n");

        for (String line : lines) {
            // Extract compilation info
            Matcher compiledMatcher = MAVEN_COMPILED_PATTERN.matcher(line);
            if (compiledMatcher.find()) {
                int count = Integer.parseInt(compiledMatcher.group(1));
                compiledFiles.add(count + " source files compiled");
            }

            // Extract errors
            Matcher errorMatcher = MAVEN_ERROR_PATTERN.matcher(line);
            if (errorMatcher.find()) {
                String error = errorMatcher.group(1).trim();
                if (!error.isEmpty() && !error.contains("BUILD FAILURE")) {
                    errors.add(error);
                }
            }

            // Extract warnings
            Matcher warningMatcher = MAVEN_WARNING_PATTERN.matcher(line);
            if (warningMatcher.find()) {
                String warning = warningMatcher.group(1).trim();
                if (!warning.isEmpty()) {
                    warnings.add(warning);
                }
            }
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

}
