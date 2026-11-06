plugins { java }
group = "dev.sentinel"
version = "1.0.0"
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies { compileOnly("io.papermc.paper:paper-api:26.3.build.+") }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
tasks.processResources { filesMatching("plugin.yml") { expand("version" to project.version) } }
