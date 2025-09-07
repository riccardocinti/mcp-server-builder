package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.exceptions.ArtifactDiscoveryException;
import com.riccardocinti.mcp_server_build.model.ArtifactInfo;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.CompilationResult;
import com.riccardocinti.mcp_server_build.service.interfaces.ArtifactDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class ArtifactDiscoveryServiceImpl implements ArtifactDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactDiscoveryServiceImpl.class);

    private static final Pattern JAR_PATTERN = Pattern.compile(".*\\.(jar|war|ear)$");
    private static final Pattern EXECUTABLE_JAR_PATTERN = Pattern.compile(".*-(?:exec|fat|uber|all|executable)\\.jar$");
    private static final Pattern SPRING_BOOT_JAR_PATTERN = Pattern.compile(".*(?<!-sources)(?<!-javadoc)\\.jar$");
    private static final Pattern SOURCE_JAR_PATTERN = Pattern.compile(".*-sources\\.jar$");
    private static final Pattern JAVADOC_JAR_PATTERN = Pattern.compile(".*-javadoc\\.jar$");

    @Override
    public ArtifactInfo discoverArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult)
            throws ArtifactDiscoveryException {
        logger.debug("Starting artifact discovery for {} project", builderConfiguration.buildTool());

        try {
            return switch (builderConfiguration.buildTool()) {
                case MAVEN -> discoverMavenArtifacts(builderConfiguration, compilationResult);
                case GRADLE, GRADLE_KOTLIN -> discoverGradleArtifacts(builderConfiguration, compilationResult);
                case NPM -> discoverNpmArtifacts(builderConfiguration, compilationResult);
            };
        } catch (Exception e) {
            throw new ArtifactDiscoveryException("Failed to discover artifacts for " + builderConfiguration.buildTool(), e);
        }

    }

    private ArtifactInfo discoverMavenArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult)
            throws IOException {
        logger.debug("Discovering Maven artifacts");

        Path projectPath = Paths.get(builderConfiguration.projectPath());
        Path targetDir = projectPath.resolve("target");

        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            logger.warn("Maven target directory not found: {}", targetDir);
            return ArtifactInfo.builder()
                    .artifacts(new ArrayList<>())
                    .totalSize(0L)
                    .build();
        }

        List<ArtifactInfo.Artifact> artifacts = new ArrayList<>();

        try (Stream<Path> files = Files.walk(targetDir, 2)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> JAR_PATTERN.matcher(path.getFileName().toString()).matches())
                    .forEach(jarPath -> {
                        try {
                            ArtifactInfo.Artifact artifact = createJarArtifact(jarPath, builderConfiguration);
                            artifacts.add(artifact);
                            logger.debug("Discovered Maven artifact: {} ({})", artifact.path(), artifact.type());
                        } catch (Exception e) {
                            logger.warn("Failed to process Maven artifact: {}", jarPath, e);
                        }
                    });
        }

        // Sort artifacts by priority (executable JARs first)
        artifacts.sort(this::compareArtifactPriority);

        // Determine main artifact
        String mainArtifactPath = findMainArtifact(artifacts, builderConfiguration);
        long totalSize = artifacts.stream().mapToLong(a -> a.size()).sum();

        ArtifactInfo artifactInfo = ArtifactInfo.builder()
                .artifacts(artifacts)
                .mainArtifactPath(mainArtifactPath)
                .totalSize(totalSize)
                .build();

        logger.info("Maven artifact discovery completed - Found {} artifacts, total size: {} MB",
                artifacts.size(), totalSize / (1024 * 1024));

        return artifactInfo;
    }

    private String findMainArtifact(List<ArtifactInfo.Artifact> artifacts, BuilderConfiguration buildConfig) {
        if (artifacts.isEmpty()) {
            return null;
        }

        // For Java projects, prefer executable JARs
        return artifacts.stream()
                .filter(a -> a.type().equals("spring-boot-jar") || a.type().equals("executable-jar"))
                .findFirst()
                .orElse(artifacts.get(0))
                .path();
    }

    private int compareArtifactPriority(ArtifactInfo.Artifact a, ArtifactInfo.Artifact b) {
        // Priority order for Java artifacts
        return getJarTypePriority(a.type()) - getJarTypePriority(b.type());
    }

    private int getJarTypePriority(String type) {
        return switch (type) {
            case "spring-boot-jar" -> 1;
            case "executable-jar" -> 2;
            case "web-archive" -> 3;
            case "enterprise-archive" -> 4;
            case "library-jar" -> 5;
            case "source-jar" -> 6;
            case "javadoc-jar" -> 7;
            default -> 8;
        };
    }

    private ArtifactInfo.Artifact createJarArtifact(Path jarPath, BuilderConfiguration builderConfiguration)
            throws IOException, NoSuchAlgorithmException {
        String fileName = jarPath.getFileName().toString();
        long size = Files.size(jarPath);
        String checksum = calculateFileChecksum(jarPath);

        // Determine artifact type
        String type = determineJarType(jarPath, builderConfiguration);

        return ArtifactInfo.Artifact.builder()
                .path(jarPath.toString())
                .type(type)
                .size(size)
                .checksum(checksum)
                .build();
    }

    private String determineJarType(Path jarPath, BuilderConfiguration buildConfig) throws IOException {
        String fileName = jarPath.getFileName().toString();

        // Check for source and javadoc JARs first
        if (SOURCE_JAR_PATTERN.matcher(fileName).matches()) {
            return "source-jar";
        }
        if (JAVADOC_JAR_PATTERN.matcher(fileName).matches()) {
            return "javadoc-jar";
        }

        // Check for executable JARs
        if (EXECUTABLE_JAR_PATTERN.matcher(fileName).matches()) {
            return "executable-jar";
        }

        // Check if it's a Spring Boot executable JAR by examining manifest
        if (isSpringBootExecutableJar(jarPath)) {
            return "spring-boot-jar";
        }

        // Check if it's an executable JAR by examining manifest
        if (isExecutableJar(jarPath)) {
            return "executable-jar";
        }

        // Determine by file extension
        if (fileName.endsWith(".war")) {
            return "web-archive";
        } else if (fileName.endsWith(".ear")) {
            return "enterprise-archive";
        } else {
            return "library-jar";
        }
    }

    private boolean isSpringBootExecutableJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                String startClass = manifest.getMainAttributes().getValue("Start-Class");

                return "org.springframework.boot.loader.JarLauncher".equals(mainClass) ||
                        "org.springframework.boot.loader.WarLauncher".equals(mainClass) ||
                        startClass != null;
            }
        } catch (IOException e) {
            logger.debug("Could not examine JAR manifest: {}", jarPath, e);
        }
        return false;
    }

    private boolean isExecutableJar(Path jarPath) {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String mainClass = manifest.getMainAttributes().getValue("Main-Class");
                return mainClass != null && !mainClass.trim().isEmpty();
            }
        } catch (IOException e) {
            logger.debug("Could not examine JAR manifest: {}", jarPath, e);
        }
        return false;
    }

    private String calculateFileChecksum(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] hashBytes = md.digest(fileBytes);
        return HexFormat.of().formatHex(hashBytes);
    }

    private ArtifactInfo discoverGradleArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult) {
        throw new UnsupportedOperationException();
    }

    private ArtifactInfo discoverNpmArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult) {
        throw new UnsupportedOperationException();
    }
}
