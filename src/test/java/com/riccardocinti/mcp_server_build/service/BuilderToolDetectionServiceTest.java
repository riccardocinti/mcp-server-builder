package com.riccardocinti.mcp_server_build.service;

import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.ProjectInfo;
import com.riccardocinti.mcp_server_build.model.enums.BuildTool;
import com.riccardocinti.mcp_server_build.service.impl.BuilderToolDetectionServiceImpl;
import com.riccardocinti.mcp_server_build.service.interfaces.BuilderToolDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.AssertionsKt.assertNotNull;

public class BuilderToolDetectionServiceTest {

    private static final String PROJECT_PATH_TEST = getTestPath("maven-project");
    private static final String PROJECT_NAME_TEST = "test-project";
    private static final String PROJECT_VERSION_TEST = "1.0.0";
    private static final String BUILD_FILE_PATH_TEST = getTestPath("maven-project/pom.xml");

    private final BuilderToolDetectionService builderToolDetectionService = new BuilderToolDetectionServiceImpl();

    @Nested
    @DisplayName("detectAndAnalyzeBuildTool(ProjectInfo projectInfo) Tests")
    class ToolDetectionAndAnalysis {

        @Test
        @DisplayName("Should successfully detect and analyse a maven project")
        void shouldSuccessfullyDetectAndAnalyseMavenProject() {
            ProjectInfo mavenProjectInfo = buildProjectInfo(BuildTool.MAVEN);
            BuilderConfiguration result = builderToolDetectionService.detectAndAnalyzeBuildTool(mavenProjectInfo);
            assertEquals(BuildTool.MAVEN, result.buildTool());
            assertEquals("21", result.javaVersion());
            assertNotNull(result.buildCommands());
            assertNotNull(result.dockerHints());
        }

    }

    private ProjectInfo buildProjectInfo(BuildTool buildTool) {
        return new ProjectInfo(
                PROJECT_PATH_TEST,
                PROJECT_NAME_TEST,
                PROJECT_VERSION_TEST,
                buildTool,
                BUILD_FILE_PATH_TEST,
                List.of());
    }

    private static String getTestPath(String relativePath) {
        return Paths.get("src/test/resources/testdata", relativePath)
                .toAbsolutePath().toString();
    }

}
