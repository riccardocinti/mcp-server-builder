package com.riccardocinti.mcp_server_build.service;

import com.riccardocinti.mcp_server_build.exceptions.*;
import com.riccardocinti.mcp_server_build.model.*;
import com.riccardocinti.mcp_server_build.model.enums.ErrorType;
import com.riccardocinti.mcp_server_build.service.interfaces.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BuilderService {

    private static final Logger logger = LoggerFactory.getLogger(BuilderService.class);

    private final ProjectDiscoveryService projectDiscoveryService;

    private final BuilderToolDetectionService builderToolDetectionService;

    private final BuildEnvironmentService buildEnvironmentService;

    private final DependencyResolutionService dependencyResolutionService;

    private final CompilationService compilationService;

    private final ArtifactDiscoveryService artifactDiscoveryService;

    private final BuildResultService buildResultService;

    public BuilderService(ProjectDiscoveryService projectDiscoveryService,
                          BuilderToolDetectionService builderToolDetectionService,
                          BuildEnvironmentService buildEnvironmentService,
                          DependencyResolutionService dependencyResolutionService,
                          CompilationService compilationService,
                          ArtifactDiscoveryService artifactDiscoveryService,
                          BuildResultService buildResultService) {
        this.projectDiscoveryService = projectDiscoveryService;
        this.builderToolDetectionService = builderToolDetectionService;
        this.buildEnvironmentService = buildEnvironmentService;
        this.dependencyResolutionService = dependencyResolutionService;
        this.compilationService = compilationService;
        this.artifactDiscoveryService = artifactDiscoveryService;
        this.buildResultService = buildResultService;
    }

    public BuildResult buildProject(String projectPath) {
        logger.info("Starting build process for project: {}", projectPath);

        try {
            logger.debug("Step 1: Discovering and validating project structure");
            ProjectInfo projectInfo = projectDiscoveryService.discoverAndValidateProject(projectPath);

            logger.debug("Step 2: Detecting build tool and analyzing configuration");
            BuilderConfiguration builderConfig = builderToolDetectionService.detectAndAnalyzeBuildTool(projectInfo);

            logger.debug("Step 3: Preparing build environment");
            BuildEnvironment buildEnv = buildEnvironmentService.prepareBuildEnvironment(builderConfig);

            logger.debug("Step 4: Resolving dependencies");
            DependencyResult depResult = dependencyResolutionService.resolveDependencies(builderConfig, buildEnv);

            logger.debug("Step 5: Executing compilation and packaging");
            CompilationResult compResult = compilationService.compileAndPackage(builderConfig, buildEnv);

            logger.debug("Step 6: Discovering generated artifacts");
            ArtifactInfo artifactInfo = artifactDiscoveryService.discoverArtifacts(builderConfig, compResult);

            logger.debug("Step 7: Compiling final build result");
            BuildResult result = buildResultService.compileBuildResult(
                    projectInfo, builderConfig, buildEnv, depResult, compResult, artifactInfo
            );

            logger.info("Build process completed successfully for project: {}", projectPath);
            return result;

        } catch (ProjectDiscoveryException e) {
            logger.error("Project discovery failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.PROJECT_DISCOVERY_FAILED, e, projectPath);

        } catch (BuilderToolDetectionException e) {
            logger.error("Build tool detection failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.BUILD_TOOL_DETECTION_FAILED, e, projectPath);

        } catch (BuildEnvironmentException e) {
            logger.error("Build environment preparation failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.BUILD_ENVIRONMENT_FAILED, e, projectPath);

        } catch (DependencyResolutionException e) {
            logger.error("Dependency resolution failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.DEPENDENCY_RESOLUTION_FAILED, e, projectPath);

        } catch (CompilationException e) {
            logger.error("Compilation failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.COMPILATION_FAILED, e, projectPath);

        } catch (ArtifactDiscoveryException e) {
            logger.error("Artifact discovery failed: {}", e.getMessage());
            return buildResultService.createFailureResult(ErrorType.ARTIFACT_DISCOVERY_FAILED, e, projectPath);

        } catch (Exception e) {
            logger.error("Unexpected error during build process: {}", e.getMessage(), e);
            return buildResultService.createFailureResult(ErrorType.UNEXPECTED_ERROR, e, projectPath);
        }
    }
}
