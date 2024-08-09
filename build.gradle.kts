import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants.Constraints
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java") // Java 지원
    alias(libs.plugins.kotlin) // Kotlin 지원
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// 프로젝트의 의존성 설정
repositories {
    mavenCentral()

    // JetBrains의 플러그인 및 라이브러리 종속성을 위해 Maven 저장소 추가
    maven {
        url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies")
    }

    // IntelliJ Platform Gradle Plugin Repositories Extension
    intellijPlatform {
        defaultRepositories()
    }
}

// 의존성은 Gradle 버전 카탈로그로 관리
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Markdown 플러그인을 번들 플러그인에 추가
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') + "org.intellij.plugins.markdown" })

        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }

    // OkHttp와 JSON 라이브러리 의존성 추가
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.vladsch.flexmark:flexmark-all:0.62.2")
    implementation("org.json:json:20231013")
    implementation("com.google.code.gson:gson:2.11.0")
}

// IntelliJ Platform Gradle Plugin 설정
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // README.md에서 플러그인 설명 섹션 추출
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // 로컬 변수로 설정 캐시 호환성 확보
        // 최신 변경 사항을 changelog 파일에서 가져오기
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // pre-release 레이블 지정
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

}

// Gradle Changelog Plugin 설정
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

// Gradle Kover Plugin 설정
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

// 기타 태스크 설정
tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// UI 테스트를 위한 IntelliJ Platform 실행 설정
val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        }
    }

    plugins {
        robotServerPlugin(Constraints.LATEST_VERSION)
    }
}