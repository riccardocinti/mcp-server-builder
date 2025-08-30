# MCP Server Build Project

A Model Context Protocol (MCP) server that provides build automation capabilities for software projects. This server can analyze, compile, and package projects using various build tools including Maven, Gradle, and NPM.

## Features

- **Multi Build Tool Support**: Maven, Gradle, NPM
- **Smart Project Analysis**: Automatic detection of build tool and configuration
- **Comprehensive Error Handling**: Detailed failure analysis and recovery suggestions
- **Rich Build Metadata**: Complete artifact information for downstream processes

## Tools

### `buildProject`
Analyzes and builds a software project in a specified directory.

**Parameters:**
- `projectPath` (string, required): Absolute path to the project directory

**Returns:**
```json
{
  "success": true,
  "buildTool": "maven",
  "javaVersion": "21",
  "projectName": "spring-petclinic",
  "projectVersion": "3.3.0",
  "artifacts": [
    {
      "path": "/path/to/project/target/spring-petclinic-3.3.0.jar",
      "type": "executable-jar",
      "size": "45.2MB"
    }
  ],
  "buildDuration": "2m 15s",
  "mainClass": "org.springframework.samples.petclinic.PetClinicApplication",
  "port": "8080",
  "buildCommands": ["mvn clean compile", "mvn package"],
  "dockerHints": {
    "baseImage": "openjdk:21-jre-slim",
    "exposedPort": "8080",
    "healthCheckPath": "/actuator/health"
  }
}
```

**Error Response:**
```json
{
  "success": false,
  "errorType": "BUILD_TOOL_DETECTION_FAILED",
  "message": "No supported build tool found in project directory",
  "suggestions": ["Ensure pom.xml, build.gradle, or package.json exists in the project root"]
}
```

## Supported Project Types

| Build Tool | Configuration File | Java Versions | Notes |
|------------|-------------------|---------------|-------|
| Maven | `pom.xml` | 8, 11, 17, 21 | Spring Boot auto-detection |
| Gradle | `build.gradle`, `build.gradle.kts` | 8, 11, 17, 21 | Kotlin DSL support |
| NPM | `package.json` | Node 16, 18, 20, 22 | React, Vue, Angular detection |

## Prerequisites

- Java 21 or higher
- Maven 3.9+ (for Maven projects)
- Gradle 8.0+ (for Gradle projects)  
- Node.js 18+ and NPM 9+ (for NPM projects)
- Docker (if using with Docker MCP Server)

## Installation

### Using Maven
```bash
git clone https://github.com/riccardocinti/mcp-server-build-project.git
cd mcp-server-build-project
mvn clean install
```

## Configuration

### Application Properties
```properties
# application.yml
mcp:
  build:
    timeout: 300000  # 5 minutes
    max-concurrent-builds: 3
    temp-directory: "/tmp/mcp-builds"
    preserve-artifacts: true

# Virtual threads configuration  
spring:
  threads:
    virtual:
      enabled: true
```

### Environment Variables
```bash
export MCP_BUILD_TIMEOUT=300000
export MCP_BUILD_TEMP_DIR="/tmp/mcp-builds"
export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
export MAVEN_HOME="/usr/share/maven"
export GRADLE_HOME="/usr/share/gradle"
```

## Usage Example

### With MCP Client
```json
{
  "method": "tools/call",
  "params": {
    "name": "buildProject",
    "arguments": {
      "projectPath": "/home/user/projects/spring-petclinic"
    }
  }
}
```

### With Spring AI Integration
```java
@RestController
public class BuildController {
    
    @Autowired
    private ChatClient chatClient;
    
    @PostMapping("/build")
    public ResponseEntity<String> buildProject(@RequestBody BuildRequest request) {
        return chatClient.prompt()
            .user("Build the project at: " + request.getProjectPath())
            .functions("buildProject")
            .call()
            .content();
    }
}
```

## Integration with Other MCP Servers

This Build MCP Server is designed to work seamlessly with:

- **GitHub MCP Server**: Clone → Build workflow
- **Docker MCP Server**: Build → Containerize workflow  
- **Deployment MCP Servers**: Build → Deploy workflow

### Complete Workflow Example
```bash
# 1. Clone with GitHub MCP
curl -X POST http://localhost:8080/mcp/github/clone \
  -d '{"repositoryUrl": "https://github.com/spring-projects/spring-petclinic.git"}'

# 2. Build with Build MCP  
curl -X POST http://localhost:8080/mcp/build/project \
  -d '{"projectPath": "/tmp/spring-petclinic"}'

# 3. Dockerize with Docker MCP
curl -X POST http://localhost:8080/mcp/docker/containerize \
  -d '{"buildResult": "..."}'
```

## Error Handling

The server provides detailed error categorization:

| Error Type | Description | Common Causes |
|------------|-------------|---------------|
| `PROJECT_DISCOVERY_FAILED` | Cannot find or access project | Invalid path, permissions |
| `BUILD_TOOL_DETECTION_FAILED` | No supported build tool found | Missing build files |
| `BUILD_ENVIRONMENT_FAILED` | Build environment setup issues | Missing tools, wrong versions |
| `DEPENDENCY_RESOLUTION_FAILED` | Cannot resolve dependencies | Network issues, missing repos |
| `COMPILATION_FAILED` | Source code compilation errors | Syntax errors, missing deps |
| `ARTIFACT_DISCOVERY_FAILED` | Cannot find generated artifacts | Build incomplete, wrong paths |

## Performance

- **Streaming Output**: Real-time build log streaming for long-running builds
- **Resource Management**: Automatic cleanup of temporary build artifacts
- **Build Caching**: Leverages build tool native caching (Maven local repo, Gradle cache)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Related Projects

- [MCP Server GitHub](https://github.com/riccardocinti/mcp-server-github) - Git repository operations
- [MCP Server Docker](https://github.com/riccardocinti/mcp-server-docker) - Container management operations

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   MCP Client    │───▶│  Build MCP       │───▶│ External Tools  │
│  (Spring AI)    │    │    Server        │    │ (Maven/Gradle)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │  Build Result   │
                       │   Artifacts     │
                       └─────────────────┘
```
