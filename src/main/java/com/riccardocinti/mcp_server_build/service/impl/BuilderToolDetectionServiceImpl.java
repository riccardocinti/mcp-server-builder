package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.exceptions.BuilderToolDetectionException;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.DockerHints;
import com.riccardocinti.mcp_server_build.model.ProjectInfo;
import com.riccardocinti.mcp_server_build.model.enums.BuildTool;
import com.riccardocinti.mcp_server_build.service.interfaces.BuilderToolDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.riccardocinti.mcp_server_build.model.enums.BuildTool.MAVEN;

@Service
public class BuilderToolDetectionServiceImpl implements BuilderToolDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(BuilderToolDetectionServiceImpl.class);

    // Common patterns for detecting frameworks and configurations
    private static final Pattern SPRING_BOOT_PATTERN = Pattern.compile("spring-boot-starter|@SpringBootApplication");
    private static final Pattern JAVA_VERSION_PATTERN = Pattern.compile("(?:java\\.version|maven\\.compiler\\.source|sourceCompatibility).*?([0-9]+)");
    private static final Pattern SERVER_PORT_PATTERN = Pattern.compile("server\\.port\\s*[:=]\\s*([0-9]+)");
    private static final Pattern MAIN_CLASS_PATTERN = Pattern.compile("@SpringBootApplication|public static void main");

    public BuilderConfiguration detectAndAnalyzeBuildTool(ProjectInfo projectInfo) throws BuilderToolDetectionException {
        logger.debug("Analyzing build tool configuration for {} project: {}",
                projectInfo.buildTool(), projectInfo.projectName());

        try {
            return switch (projectInfo.buildTool()) {
                case MAVEN -> analyzeMavenProject(projectInfo);
                case GRADLE, GRADLE_KOTLIN -> analyzeGradleProject(projectInfo);
                case NPM -> analyzeNpmProject(projectInfo);
            };
        } catch (Exception e) {
            throw new BuilderToolDetectionException(
                    "Failed to analyze build configuration for " + projectInfo.buildTool() + " project", e
            );
        }
    }

    private BuilderConfiguration analyzeMavenProject(ProjectInfo projectInfo) throws IOException {
        logger.debug("Analyzing Maven project configuration");

        Path pomPath = Paths.get(projectInfo.buildFilePath());
        String pomContent = Files.readString(pomPath);
        Path projectRoot = pomPath.getParent();

        BuilderConfiguration.Builder configBuilder = BuilderConfiguration.builder()
                .buildTool(MAVEN)
                .projectPath(projectInfo.projectPath());

        // Extract Java version
        String javaVersion = extractJavaVersionFromMaven(pomContent);
        configBuilder.javaVersion(javaVersion);

        // Determine if it's a Spring Boot project
        boolean isSpringBoot = isSpringBootProject(pomContent, projectRoot);

        // Extract main class
        String mainClass = extractMainClass(projectRoot, isSpringBoot, projectInfo.projectName());
        if (mainClass != null) {
            configBuilder.mainClass(mainClass);
        }

        // Extract or determine server port
        String port = extractServerPort(projectRoot, isSpringBoot);
        configBuilder.port(port);

        // Build commands based on project type
        List<String> buildCommands = generateMavenBuildCommands(isSpringBoot);
        configBuilder.buildCommands(buildCommands);

        // Extract additional properties
        Map<String, String> properties = extractMavenProperties(pomContent);
        configBuilder.properties(properties);

        // Generate Docker hints
        DockerHints dockerHints = generateDockerHints(javaVersion, port, isSpringBoot, MAVEN);
        configBuilder.dockerHints(dockerHints);

        logger.info("Maven project analysis completed - Java {}, Spring Boot: {}, Port: {}",
                javaVersion, isSpringBoot, port);

        return configBuilder.build();
    }

    private BuilderConfiguration analyzeNpmProject(ProjectInfo projectInfo) {
        throw new UnsupportedOperationException();
    }

    private BuilderConfiguration analyzeGradleProject(ProjectInfo projectInfo) {
        throw new UnsupportedOperationException();
    }

    private String extractJavaVersionFromMaven(String pomContent) {
        // Look for maven.compiler.source or java.version
        Pattern[] patterns = {
                Pattern.compile("<maven\\.compiler\\.source>([0-9]+)</maven\\.compiler\\.source>"),
                Pattern.compile("<java\\.version>([0-9]+)</java\\.version>"),
                Pattern.compile("<maven\\.compiler\\.target>([0-9]+)</maven\\.compiler\\.target>")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(pomContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        logger.warn("Could not determine Java version from pom.xml, defaulting to 21");
        return "21"; // Default to Java 21
    }

    private boolean isSpringBootProject(String buildContent, Path projectRoot) {
        // Check build file content for Spring Boot dependencies
        if (SPRING_BOOT_PATTERN.matcher(buildContent).find()) {
            return true;
        }

        // Check for Spring Boot application class
        try {
            return Files.walk(projectRoot)
                    .filter(path -> path.toString().endsWith(".java"))
                    .anyMatch(javaFile -> {
                        try {
                            String content = Files.readString(javaFile);
                            return content.contains("@SpringBootApplication");
                        } catch (IOException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            logger.warn("Error checking for Spring Boot application class", e);
            return false;
        }
    }

    private String extractMainClass(Path projectRoot, boolean isSpringBoot, String projectName) {
        if (!isSpringBoot) {
            return null; // Non-Spring Boot projects might not have a clear main class
        }

        try {
            // Look for @SpringBootApplication annotated class
            Optional<Path> mainClassPath = Files.walk(projectRoot)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(javaFile -> {
                        try {
                            String content = Files.readString(javaFile);
                            return content.contains("@SpringBootApplication");
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .findFirst();

            if (mainClassPath.isPresent()) {
                return extractFullyQualifiedClassName(mainClassPath.get(), projectRoot);
            }
        } catch (IOException e) {
            logger.warn("Error extracting main class", e);
        }

        // Fallback: generate likely main class name
        String capitalizedProjectName = projectName.substring(0, 1).toUpperCase() +
                projectName.substring(1).replaceAll("[^a-zA-Z0-9]", "");
        return "com.example." + capitalizedProjectName.toLowerCase() + "." + capitalizedProjectName + "Application";
    }

    private String extractFullyQualifiedClassName(Path javaFile, Path projectRoot) throws IOException {
        String content = Files.readString(javaFile);

        // Extract package name
        Pattern packagePattern = Pattern.compile("package\\s+([^;]+);");
        Matcher packageMatcher = packagePattern.matcher(content);
        String packageName = packageMatcher.find() ? packageMatcher.group(1).trim() : "";

        // Extract class name from file name
        String fileName = javaFile.getFileName().toString();
        String className = fileName.substring(0, fileName.lastIndexOf('.'));

        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    private String extractServerPort(Path projectRoot, boolean isSpringBoot) {
        if (!isSpringBoot) {
            return "3000"; // Default for non-Spring Boot projects
        }

        // Check application.properties and application.yml
        String[] configFiles = {"application.properties", "application.yml", "application.yaml"};

        for (String configFile : configFiles) {
            Path configPath = projectRoot.resolve("src/main/resources/" + configFile);
            if (Files.exists(configPath)) {
                try {
                    String content = Files.readString(configPath);
                    Matcher matcher = SERVER_PORT_PATTERN.matcher(content);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } catch (IOException e) {
                    logger.warn("Error reading config file: " + configFile, e);
                }
            }
        }

        return "8080"; // Default Spring Boot port
    }

    private List<String> generateMavenBuildCommands(boolean isSpringBoot) {
        List<String> commands = new ArrayList<>();
        commands.add("mvn clean compile");

        if (isSpringBoot) {
            commands.add("mvn spring-boot:build-image");
        } else {
            commands.add("mvn package");
        }

        return commands;
    }

    private Map<String, String> extractMavenProperties(String pomContent) {
        Map<String, String> properties = new HashMap<>();

        // Extract properties section
        Pattern propertiesPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher matcher = propertiesPattern.matcher(pomContent);

        if (matcher.find()) {
            String propertiesSection = matcher.group(1);
            Pattern propertyPattern = Pattern.compile("<([^>]+)>([^<]+)</[^>]+>");
            Matcher propertyMatcher = propertyPattern.matcher(propertiesSection);

            while (propertyMatcher.find()) {
                properties.put(propertyMatcher.group(1), propertyMatcher.group(2));
            }
        }

        return properties;
    }

    private DockerHints generateDockerHints(String javaVersion, String port, boolean isSpringBoot, BuildTool buildTool) {
        DockerHints.Builder hintsBuilder = DockerHints.builder();

        // Base image based on Java version
        String baseImage = switch (javaVersion) {
            case "8" -> "openjdk:8-jre-slim";
            case "11" -> "openjdk:11-jre-slim";
            case "17" -> "openjdk:17-jre-slim";
            case "21" -> "openjdk:21-jre-slim";
            default -> "openjdk:21-jre-slim";
        };

        hintsBuilder.baseImage(baseImage);
        hintsBuilder.exposedPort(port);
        hintsBuilder.workDir("/app");

        if (isSpringBoot) {
            hintsBuilder.healthCheckPath("/actuator/health");
            hintsBuilder.startCommand("java -jar app.jar");
        } else {
            hintsBuilder.startCommand("java -jar app.jar");
        }

        return hintsBuilder.build();
    }
}
