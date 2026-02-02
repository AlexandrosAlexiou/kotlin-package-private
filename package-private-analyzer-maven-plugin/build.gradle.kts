plugins {
  kotlin("jvm")
  id("maven-publish")
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(project(":package-private-analyzer-core"))
  
  // Maven plugin API
  compileOnly("org.apache.maven:maven-plugin-api:3.9.6")
  compileOnly("org.apache.maven:maven-core:3.9.6")
  compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.11.0")
  
  testImplementation("org.jetbrains.kotlin:kotlin-test:2.3.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test { useJUnitPlatform() }

// Generate Maven plugin descriptor
val generatePluginDescriptor by tasks.registering {
    val descriptorFile = file("$buildDir/generated-resources/META-INF/maven/plugin.xml")
    outputs.file(descriptorFile)
    
    doLast {
        descriptorFile.apply {
            parentFile.mkdirs()
            writeText("""<?xml version="1.0" encoding="UTF-8"?>
<plugin>
    <name>Package Private Analyzer Maven Plugin</name>
    <description>Maven plugin for analyzing package-private candidates in Kotlin code</description>
    <groupId>dev.packageprivate.analyzer</groupId>
    <artifactId>package-private-analyzer-maven-plugin</artifactId>
    <version>${project.version}</version>
    <goalPrefix>package-private</goalPrefix>
    <isolatedRealm>false</isolatedRealm>
    <inheritedByDefault>true</inheritedByDefault>
    <mojos>
        <mojo>
            <goal>analyze</goal>
            <description>Analyzes Kotlin source files to find candidates for @PackagePrivate annotation</description>
            <requiresProject>true</requiresProject>
            <threadSafe>true</threadSafe>
            <phase>verify</phase>
            <implementation>dev.packageprivate.analyzer.maven.AnalyzePackagePrivateCandidatesMojo</implementation>
            <language>java</language>
            <instantiationStrategy>per-lookup</instantiationStrategy>
            <executionStrategy>once-per-session</executionStrategy>
            <parameters>
                <parameter>
                    <name>project</name>
                    <type>org.apache.maven.project.MavenProject</type>
                    <required>false</required>
                    <editable>false</editable>
                    <description>The Maven project</description>
                    <defaultValue>${'$'}{project}</defaultValue>
                </parameter>
                <parameter>
                    <name>includePublic</name>
                    <type>boolean</type>
                    <required>false</required>
                    <editable>true</editable>
                    <description>Include public declarations in candidate analysis</description>
                    <defaultValue>true</defaultValue>
                </parameter>
                <parameter>
                    <name>includeInternal</name>
                    <type>boolean</type>
                    <required>false</required>
                    <editable>true</editable>
                    <description>Include internal declarations in candidate analysis</description>
                    <defaultValue>true</defaultValue>
                </parameter>
                <parameter>
                    <name>outputFile</name>
                    <type>java.io.File</type>
                    <required>false</required>
                    <editable>true</editable>
                    <description>Output file for the analysis report</description>
                    <defaultValue>${'$'}{project.build.directory}/reports/package-private-candidates.txt</defaultValue>
                </parameter>
                <parameter>
                    <name>sourceDirectories</name>
                    <type>java.util.List</type>
                    <required>false</required>
                    <editable>true</editable>
                    <description>Source directories to analyze</description>
                </parameter>
            </parameters>
            <configuration>
                <project implementation="org.apache.maven.project.MavenProject" default-value="${'$'}{project}"/>
                <includePublic implementation="boolean" default-value="true">${'$'}{packageprivate.analyzer.includePublic}</includePublic>
                <includeInternal implementation="boolean" default-value="true">${'$'}{packageprivate.analyzer.includeInternal}</includeInternal>
                <outputFile implementation="java.io.File" default-value="${'$'}{project.build.directory}/reports/package-private-candidates.txt">${'$'}{packageprivate.analyzer.outputFile}</outputFile>
                <sourceDirectories implementation="java.util.List">${'$'}{packageprivate.analyzer.sourceDirectories}</sourceDirectories>
            </configuration>
        </mojo>
    </mojos>
    <dependencies>
        <dependency>
            <groupId>dev.packageprivate.analyzer</groupId>
            <artifactId>package-private-analyzer-core</artifactId>
            <type>jar</type>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</plugin>
""")
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir("$buildDir/generated-resources")
        }
    }
}

tasks.named("processResources") {
    dependsOn(generatePluginDescriptor)
}

publishing {
  repositories {
    mavenLocal()
  }
}
