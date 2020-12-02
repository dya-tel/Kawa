package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources

class AzureMavenPluginsTest : DiffTesterBase() {
    override val model = ModelLoader(
            "azure-maven-plugins",
            collectSources(baseProjectPath.resolve("azure-maven-plugins-develop"))
    )
}