package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.config.BuilderServerConfig;
import com.riccardocinti.mcp_server_build.exceptions.BuildEnvironmentException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.enums.BuildTool;
import com.riccardocinti.mcp_server_build.service.interfaces.BuildEnvironmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class BuildEnvironmentServiceImpl implements BuildEnvironmentService {

    private static final Logger logger = LoggerFactory.getLogger(BuildEnvironmentServiceImpl.class);

    private final BuilderServerConfig builderServerConfig;

    public BuildEnvironmentServiceImpl(BuilderServerConfig builderServerConfig) {
        this.builderServerConfig = builderServerConfig;
    }

    @Override
    public BuildEnvironment prepareBuildEnvironment(BuilderConfiguration builderConfig) throws BuildEnvironmentException {
        logger.debug("Preparing build environment for {} project", builderConfig.buildTool());

        try {
            BuildEnvironment.Builder envBuilder = BuildEnvironment.builder();

            String workingDirectory = setupWorkingDirectory(builderConfig);
            envBuilder.workingDirectory(workingDirectory);

            String tempDirectory = setupTempDirectory();
            envBuilder.tempDirectory(tempDirectory);

            Map<String, String> environmentVariables = prepareEnvironmentVariables(builderConfig);
            envBuilder.environmentVariables(environmentVariables);

            validateAndSetToolHomes(builderConfig, envBuilder, environmentVariables);

            BuildEnvironment buildEnvironment = envBuilder.build();

            logger.info("Build environment prepared successfully for {} project", builderConfig.buildTool());
            logger.debug("Working directory: {}", workingDirectory);
            logger.debug("Temp directory: {}", tempDirectory);

            return buildEnvironment;
        } catch (Exception e) {
            throw new BuildEnvironmentException("Failed to prepare build environment", e);
        }
    }

    private String setupWorkingDirectory(BuilderConfiguration buildConfig) throws BuildEnvironmentException {
        Path projectPath = Paths.get(buildConfig.projectPath()).toAbsolutePath().normalize();

        if (!Files.exists(projectPath)) {
            throw new BuildEnvironmentException("Project path does not exist: " + projectPath);
        }

        if (!Files.isDirectory(projectPath)) {
            throw new BuildEnvironmentException("Project path is not a directory: " + projectPath);
        }

        if (!Files.isWritable(projectPath)) {
            throw new BuildEnvironmentException("Project path is not writable: " + projectPath);
        }

        logger.debug("Working directory validated: {}", projectPath);
        return projectPath.toString();
    }

    private String setupTempDirectory() throws BuildEnvironmentException {
        String tempDirPath = builderServerConfig.getTempDirectory();
        if (tempDirPath == null || tempDirPath.trim().isEmpty()) {
            tempDirPath = System.getProperty("java.io.tmpdir") + "/mcp-builds";
        }

        Path tempDir = Paths.get(tempDirPath).toAbsolutePath().normalize();

        try {
            // Create temp directory if it doesn't exist
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                logger.debug("Created temp directory: {}", tempDir);
            }

            // Verify temp directory is usable
            if (!Files.isDirectory(tempDir)) {
                throw new BuildEnvironmentException("Temp path is not a directory: " + tempDir);
            }

            if (!Files.isWritable(tempDir)) {
                throw new BuildEnvironmentException("Temp directory is not writable: " + tempDir);
            }

            return tempDir.toString();

        } catch (IOException e) {
            throw new BuildEnvironmentException("Failed to setup temp directory: " + tempDir, e);
        }
    }

    private Map<String, String> prepareEnvironmentVariables(BuilderConfiguration buildConfig) {
        Map<String, String> envVars = new HashMap<>();

        // Copy current environment variables
        envVars.putAll(System.getenv());

        // Set common build environment variables
        envVars.put("MCP_BUILD_TOOL", buildConfig.buildTool().name());
        envVars.put("MCP_BUILD_TIMEOUT", String.valueOf(builderServerConfig.getTimeout()));

        // Set tool-specific environment variables
        switch (buildConfig.buildTool()) {
            case MAVEN -> prepareMavenEnvironment(envVars, buildConfig);
            case GRADLE, GRADLE_KOTLIN -> prepareGradleEnvironment(envVars, buildConfig);
            case NPM -> prepareNpmEnvironment(envVars, buildConfig);
        }

        logger.debug("Prepared {} environment variables", envVars.size());
        return envVars;
    }

    private void prepareMavenEnvironment(Map<String, String> envVars, BuilderConfiguration buildConfig) {
        // Maven-specific environment variables
        envVars.put("MAVEN_OPTS", "-Xmx2g -XX:+UseG1GC");
        envVars.put("MAVEN_BATCH_MODE", "true");

        // Set Java version if specified
        if (buildConfig.javaVersion() != null) {
            envVars.put("JAVA_VERSION", buildConfig.javaVersion());
        }

        // Disable Maven download progress to reduce log noise
        envVars.put("MAVEN_CLI_OPTS", "--batch-mode --no-transfer-progress");

        logger.debug("Configured Maven environment variables");
    }

    private void prepareGradleEnvironment(Map<String, String> envVars, BuilderConfiguration buildConfig) {
        throw new UnsupportedOperationException();
    }

    private void prepareNpmEnvironment(Map<String, String> envVars, BuilderConfiguration buildConfig) {
        throw new UnsupportedOperationException();
    }

    private void validateAndSetToolHomes(BuilderConfiguration buildConfig, BuildEnvironment.Builder envBuilder,
                                         Map<String, String> envVars) throws BuildEnvironmentException {

        switch (buildConfig.buildTool()) {
            case MAVEN -> validateAndSetMavenHome(envBuilder, envVars);
            case GRADLE, GRADLE_KOTLIN -> validateAndSetGradleHome(envBuilder, envVars);
            case NPM -> validateAndSetNodeHome(envBuilder, envVars);
        }

        // Always validate and set JAVA_HOME for JVM-based builds
        if (buildConfig.buildTool() != BuildTool.NPM) {
            validateAndSetJavaHome(envBuilder, envVars);
        }
    }

    private void validateAndSetJavaHome(BuildEnvironment.Builder envBuilder, Map<String, String> envVars)
            throws BuildEnvironmentException {

        String javaHome = findJavaHome(envVars);

        if (javaHome == null) {
            throw new BuildEnvironmentException(
                    "JAVA_HOME not found. Please ensure Java is installed and JAVA_HOME is set."
            );
        }

        Path javaHomePath = Paths.get(javaHome);
        Path javaBinary = javaHomePath.resolve("bin/java");

        if (!Files.exists(javaBinary) && !Files.exists(javaBinary.resolveSibling("java.exe"))) {
            throw new BuildEnvironmentException(
                    "Java binary not found in JAVA_HOME: " + javaHome + "/bin/java"
            );
        }

        envBuilder.javaHome(javaHome);
        envVars.put("JAVA_HOME", javaHome);

        logger.debug("Java home validated and set: {}", javaHome);
    }

    private String findJavaHome(Map<String, String> envVars) {
        // Try JAVA_HOME environment variable first
        String javaHome = envVars.get("JAVA_HOME");
        if (javaHome != null && !javaHome.trim().isEmpty()) {
            return javaHome.trim();
        }

        // Try to detect from java.home system property
        String systemJavaHome = System.getProperty("java.home");
        if (systemJavaHome != null) {
            return systemJavaHome;
        }

        // Try common Java installation paths
        String[] commonPaths = {
                "/usr/lib/jvm/java-21-openjdk",
                "/usr/lib/jvm/java-17-openjdk",
                "/usr/lib/jvm/java-11-openjdk",
                "/usr/lib/jvm/default-java",
                "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home",
                "C:\\Program Files\\Java\\jdk-21",
                "C:\\Program Files\\OpenJDK\\openjdk-21"
        };

        for (String path : commonPaths) {
            Path javaPath = Paths.get(path);
            if (Files.exists(javaPath) && Files.isDirectory(javaPath)) {
                Path javaBinary = javaPath.resolve("bin/java");
                if (Files.exists(javaBinary) || Files.exists(javaBinary.resolveSibling("java.exe"))) {
                    logger.debug("Auto-detected Java home: {}", path);
                    return path;
                }
            }
        }

        return null;
    }

    private void validateAndSetMavenHome(BuildEnvironment.Builder envBuilder, Map<String, String> envVars)
            throws BuildEnvironmentException {

        String mavenHome = findMavenHome(envVars);

        if (mavenHome == null) {
            throw new BuildEnvironmentException(
                    "Maven not found. Please ensure Maven is installed and available in PATH or MAVEN_HOME is set."
            );
        }

        envBuilder.buildToolHome(mavenHome);
        envVars.put("MAVEN_HOME", mavenHome);

        logger.debug("Maven home validated and set: {}", mavenHome);
    }

    private String findMavenHome(Map<String, String> envVars) {
        // Try MAVEN_HOME environment variable first
        String mavenHome = envVars.get("MAVEN_HOME");
        if (mavenHome != null && validateMavenInstallation(mavenHome)) {
            return mavenHome;
        }

        // Try M2_HOME (alternative Maven home variable)
        String m2Home = envVars.get("M2_HOME");
        if (m2Home != null && validateMavenInstallation(m2Home)) {
            return m2Home;
        }

        // Try to find Maven in PATH
        String mavenFromPath = findToolInPath("mvn", envVars);
        if (mavenFromPath != null) {
            // Extract Maven home from mvn binary path
            Path mvnPath = Paths.get(mavenFromPath);
            Path mavenHomePath = mvnPath.getParent().getParent(); // ../bin/mvn -> ..
            if (validateMavenInstallation(mavenHomePath.toString())) {
                return mavenHomePath.toString();
            }
        }

        // Try common Maven installation paths
        String[] commonPaths = {
                "/usr/share/maven",
                "/opt/maven",
                "/usr/local/maven",
                "C:\\Program Files\\Apache\\Maven",
                "C:\\Maven"
        };

        for (String path : commonPaths) {
            if (validateMavenInstallation(path)) {
                logger.debug("Auto-detected Maven home: {}", path);
                return path;
            }
        }

        return null;
    }

    private boolean validateMavenInstallation(String mavenHome) {
        if (mavenHome == null || mavenHome.trim().isEmpty()) {
            return false;
        }

        Path mavenHomePath = Paths.get(mavenHome);
        Path mvnBinary = mavenHomePath.resolve("bin/mvn");
        Path mvnBinaryWindows = mavenHomePath.resolve("bin/mvn.cmd");

        return Files.exists(mvnBinary) || Files.exists(mvnBinaryWindows);
    }

    private String findToolInPath(String toolName, Map<String, String> envVars) {
        String pathVar = envVars.get("PATH");
        if (pathVar == null) {
            return null;
        }

        String pathSeparator = System.getProperty("os.name").toLowerCase().contains("windows") ? ";" : ":";
        String[] paths = pathVar.split(pathSeparator);

        for (String path : paths) {
            Path toolPath = Paths.get(path, toolName);
            Path toolPathWindows = Paths.get(path, toolName + ".exe");
            Path toolPathCmd = Paths.get(path, toolName + ".cmd");

            if (Files.exists(toolPath) || Files.exists(toolPathWindows) || Files.exists(toolPathCmd)) {
                return toolPath.toString();
            }
        }

        return null;
    }

    private void validateAndSetNodeHome(BuildEnvironment.Builder envBuilder, Map<String, String> envVars) {
        throw new UnsupportedOperationException();
    }

    private void validateAndSetGradleHome(BuildEnvironment.Builder envBuilder, Map<String, String> envVars) {
        throw new UnsupportedOperationException();
    }
}
