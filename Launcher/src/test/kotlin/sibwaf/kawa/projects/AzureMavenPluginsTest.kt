package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources
import java.nio.file.Path

class AzureMavenPluginsTest : DiffTesterBase() {
    override val rootPath: Path = baseProjectPath.resolve("azure-maven-plugins-develop")
    override val model = ModelLoader(
        "azure-maven-plugins",
        rootPath,
        collectSources(rootPath)
    )
}