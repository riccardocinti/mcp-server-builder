package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.exceptions.ProjectDiscoveryException;
import com.riccardocinti.mcp_server_build.model.BuildToolInfo;
import com.riccardocinti.mcp_server_build.model.ProjectInfo;
import com.riccardocinti.mcp_server_build.model.enums.BuildTool;
import com.riccardocinti.mcp_server_build.service.interfaces.ProjectDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectDiscoveryServiceImpl implements ProjectDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDiscoveryServiceImpl.class);

    @Override
    public ProjectInfo discoverAndValidateProject(String projectPath) throws ProjectDiscoveryException {
        logger.debug("Starting project discovery for path: {}", projectPath);

        Path projectRoot = validateProjectPath(projectPath);

        BuildToolInfo buildToolInfo = detectBuildTool(projectRoot);

        String projectName = extractProjectName(projectRoot, buildToolInfo.buildTool());
        String projectVersion = extractProjectVersion(projectRoot, buildToolInfo.buildTool());

        List<String> additionalFiles = discoverAdditionalFiles(projectRoot, buildToolInfo.buildTool());

        // Step 5: Create and return ProjectInfo
        ProjectInfo projectInfo = ProjectInfo.builder()
                .projectPath(projectPath)
                .projectName(projectName)
                .projectVersion(projectVersion)
                .buildTool(buildToolInfo.buildTool())
                .buildFilePath(buildToolInfo.buildFilePath())
                .additionalFiles(additionalFiles)
                .build();

        logger.info("Project discovery completed successfully for {} project: {}",
                buildToolInfo.buildTool(), projectName);

        return projectInfo;
    }

    private Path validateProjectPath(String projectPath) throws ProjectDiscoveryException {
        if (projectPath == null || projectPath.trim().isEmpty()) {
            throw new ProjectDiscoveryException("Project path cannot be null or empty");
        }

        Path path = Paths.get(projectPath).toAbsolutePath().normalize();

        if (!Files.exists(path)) {
            throw new ProjectDiscoveryException("Project path does not exist: " + path);
        }

        if (!Files.isDirectory(path)) {
            throw new ProjectDiscoveryException("Project path is not a directory: " + path);
        }

        if (!Files.isReadable(path)) {
            throw new ProjectDiscoveryException("Project path is not readable: " + path);
        }

        logger.debug("Project path validation successful: {}", path);
        return path;
    }

    private BuildToolInfo detectBuildTool(Path projectRoot) throws ProjectDiscoveryException {
        logger.debug("Detecting build tool in: {}", projectRoot);

        // Check for Maven
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml) && Files.isReadable(pomXml)) {
            logger.debug("Detected Maven project with pom.xml");
            return new BuildToolInfo(BuildTool.MAVEN, pomXml.toString());
        }

        // Check for Gradle Kotlin DSL first (more specific)
        Path buildGradleKts = projectRoot.resolve("build.gradle.kts");
        if (Files.exists(buildGradleKts) && Files.isReadable(buildGradleKts)) {
            logger.debug("Detected Gradle project with build.gradle.kts");
            return new BuildToolInfo(BuildTool.GRADLE_KOTLIN, buildGradleKts.toString());
        }

        // Check for Gradle Groovy DSL
        Path buildGradle = projectRoot.resolve("build.gradle");
        if (Files.exists(buildGradle) && Files.isReadable(buildGradle)) {
            logger.debug("Detected Gradle project with build.gradle");
            return new BuildToolInfo(BuildTool.GRADLE, buildGradle.toString());
        }

        // Check for NPM
        Path packageJson = projectRoot.resolve("package.json");
        if (Files.exists(packageJson) && Files.isReadable(packageJson)) {
            logger.debug("Detected NPM project with package.json");
            return new BuildToolInfo(BuildTool.NPM, packageJson.toString());
        }

        // No supported build tool found
        throw new ProjectDiscoveryException(
                "No supported build tool detected. Expected one of: pom.xml, build.gradle, build.gradle.kts, package.json"
        );
    }

    private String extractProjectName(Path projectRoot, BuildTool buildTool) {
        try {
            return switch (buildTool) {
                case MAVEN -> extractMavenProjectName(projectRoot);
                case GRADLE, GRADLE_KOTLIN -> extractGradleProjectName(projectRoot);
                case NPM -> extractNpmProjectName(projectRoot);
            };
        } catch (Exception e) {
            logger.warn("Could not extract project name, using directory name as fallback", e);
            return projectRoot.getFileName().toString();
        }
    }

    private String extractMavenProjectName(Path projectRoot) throws IOException {
        Path pomPath = projectRoot.resolve("pom.xml");
        String pomContent = Files.readString(pomPath);

        // Simple XML parsing for artifactId
        String artifactIdPattern = "<artifactId>([^<]+)</artifactId>";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(artifactIdPattern);
        java.util.regex.Matcher matcher = pattern.matcher(pomContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return projectRoot.getFileName().toString();
    }

    private String extractGradleProjectName(Path projectRoot) throws IOException {
        // Try settings.gradle first
        Path settingsGradle = projectRoot.resolve("settings.gradle");
        Path settingsGradleKts = projectRoot.resolve("settings.gradle.kts");

        Path settingsFile = Files.exists(settingsGradleKts) ? settingsGradleKts : settingsGradle;

        if (Files.exists(settingsFile)) {
            String settingsContent = Files.readString(settingsFile);

            // Look for rootProject.name
            String namePattern = "rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(namePattern);
            java.util.regex.Matcher matcher = pattern.matcher(settingsContent);

            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return projectRoot.getFileName().toString();
    }

    private String extractNpmProjectName(Path projectRoot) throws IOException {
        Path packageJsonPath = projectRoot.resolve("package.json");
        String packageJsonContent = Files.readString(packageJsonPath);

        // Simple JSON parsing for name field
        String namePattern = "\"name\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(namePattern);
        java.util.regex.Matcher matcher = pattern.matcher(packageJsonContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return projectRoot.getFileName().toString();
    }

    private String extractProjectVersion(Path projectRoot, BuildTool buildTool) {
        try {
            return switch (buildTool) {
                case MAVEN -> extractMavenProjectVersion(projectRoot);
                case GRADLE, GRADLE_KOTLIN -> extractGradleProjectVersion(projectRoot);
                case NPM -> extractNpmProjectVersion(projectRoot);
            };
        } catch (Exception e) {
            logger.warn("Could not extract project version, using default", e);
            return "unknown";
        }
    }

    private String extractMavenProjectVersion(Path projectRoot) throws IOException {
        Path pomPath = projectRoot.resolve("pom.xml");
        String pomContent = Files.readString(pomPath);

        String versionPattern = "<version>([^<]+)</version>";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(versionPattern);
        java.util.regex.Matcher matcher = pattern.matcher(pomContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "unknown";
    }

    private String extractGradleProjectVersion(Path projectRoot) throws IOException {
        Path buildFile = Files.exists(projectRoot.resolve("build.gradle.kts")) ?
                projectRoot.resolve("build.gradle.kts") : projectRoot.resolve("build.gradle");

        String buildContent = Files.readString(buildFile);

        String versionPattern = "version\\s*=\\s*['\"]([^'\"]+)['\"]";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(versionPattern);
        java.util.regex.Matcher matcher = pattern.matcher(buildContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "unknown";
    }

    private String extractNpmProjectVersion(Path projectRoot) throws IOException {
        Path packageJsonPath = projectRoot.resolve("package.json");
        String packageJsonContent = Files.readString(packageJsonPath);

        String versionPattern = "\"version\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(versionPattern);
        java.util.regex.Matcher matcher = pattern.matcher(packageJsonContent);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "unknown";
    }

    private List<String> discoverAdditionalFiles(Path projectRoot, BuildTool buildTool) {
        List<String> additionalFiles = new ArrayList<>();

        try {
            // Common files to look for based on build tool
            List<String> filesToCheck = switch (buildTool) {
                case MAVEN -> List.of(
                        "src/main/java",
                        "src/main/resources",
                        "src/test/java",
                        "mvnw",
                        "mvnw.cmd",
                        ".mvn"
                );
                case GRADLE, GRADLE_KOTLIN -> List.of(
                        "src/main/java",
                        "src/main/kotlin",
                        "src/main/resources",
                        "src/test/java",
                        "src/test/kotlin",
                        "gradlew",
                        "gradlew.bat",
                        "gradle",
                        "settings.gradle",
                        "settings.gradle.kts"
                );
                case NPM -> List.of(
                        "src",
                        "public",
                        "dist",
                        "node_modules",
                        "package-lock.json",
                        "yarn.lock",
                        "webpack.config.js",
                        "tsconfig.json",
                        ".babelrc"
                );
            };

            for (String fileToCheck : filesToCheck) {
                Path filePath = projectRoot.resolve(fileToCheck);
                if (Files.exists(filePath)) {
                    additionalFiles.add(filePath.toString());
                    logger.debug("Found additional file/directory: {}", fileToCheck);
                }
            }

        } catch (Exception e) {
            logger.warn("Error discovering additional files", e);
        }

        return additionalFiles;
    }

}
