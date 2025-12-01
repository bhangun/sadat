<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>tech.kayys.wayang.generated</groupId>
    <artifactId>${artifactId}</artifactId>
    <version>${version}</version>
    <packaging>jar</packaging>

    <name>${workflowName} - Standalone Agent</name>
    <description>
        Generated standalone agent for workflow: ${workflowName}
        Generated at: ${generationTimestamp}
        
        This agent includes only the components used in your workflow:
<#list usedNodeTypes as nodeType>
        - ${nodeType} Node
</#list>
        
        Estimated size: ${estimatedSizeMB} MB
    </description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <wayang.version>${wayangVersion}</wayang.version>
    </properties>

    <dependencies>
        <!-- ============================================ -->
        <!-- MINIMAL RUNTIME - ALWAYS INCLUDED (2MB)      -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-minimal</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>

<#if requiredCapabilities?seq_contains("LLM_ACCESS")>
        <!-- ============================================ -->
        <!-- LLM RUNTIME - Used by Agent nodes (5MB)      -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-llm</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#if>

<#if requiredCapabilities?seq_contains("RAG_QUERY")>
        <!-- ============================================ -->
        <!-- RAG RUNTIME - Used by RAG nodes (3MB)        -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-rag</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#if>

<#if requiredCapabilities?seq_contains("TOOL_EXECUTION")>
        <!-- ============================================ -->
        <!-- TOOL RUNTIME - Used by Tool nodes (2MB)      -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-tools</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#if>

<#if requiredCapabilities?seq_contains("MEMORY_ACCESS")>
        <!-- ============================================ -->
        <!-- MEMORY RUNTIME - Used by Memory nodes (1MB)  -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-memory</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#if>

<#if requiredCapabilities?seq_contains("GUARDRAILS")>
        <!-- ============================================ -->
        <!-- GUARDRAILS RUNTIME (2MB)                     -->
        <!-- ============================================ -->
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-runtime-guardrails</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#if>

        <!-- ============================================ -->
        <!-- NODE IMPLEMENTATIONS - Only used nodes       -->
        <!-- ============================================ -->
<#list usedNodeTypes as nodeType>
        <dependency>
            <groupId>tech.kayys.wayang</groupId>
            <artifactId>wayang-node-${nodeType?lower_case}</artifactId>
            <version>${r"${wayang.version}"}</version>
        </dependency>
</#list>

        <!-- ============================================ -->
        <!-- EXTERNAL DEPENDENCIES (if needed)            -->
        <!-- ============================================ -->
<#list externalDependencies as dep>
        <dependency>
            <groupId>${dep.groupId}</groupId>
            <artifactId>${dep.artifactId}</artifactId>
            <version>${dep.version}</version>
<#if dep.optional>
            <optional>true</optional>
</#if>
        </dependency>
</#list>
    </dependencies>

    <build>
        <finalName>${artifactId}</finalName>
        <plugins>
            <!-- Maven Compiler -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>

            <!-- Maven Shade - Create fat JAR -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>tech.kayys.generated.StandaloneAgent</mainClass>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

<#if generateNativeImage>
            <!-- GraalVM Native Image -->
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>0.9.28</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile-no-fork</goal>
                        </goals>
                        <phase>package</phase>
                    </execution>
                </executions>
                <configuration>
                    <imageName>${artifactId}-native</imageName>
                    <mainClass>tech.kayys.generated.StandaloneAgent</mainClass>
                    <buildArgs>
                        <buildArg>--no-fallback</buildArg>
                        <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
</#if>
        </plugins>
    </build>
</project>