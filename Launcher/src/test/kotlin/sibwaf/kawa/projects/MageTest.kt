package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources

class MageTest : DiffTesterBase() {
    override val model = ModelLoader(
        "mage",
        collectSources(
            baseProjectPath.resolve("mage-master"),
            baseProjectPath.resolve("mage-master").resolve("Mage.Server.Plugins")
        )
    )
}