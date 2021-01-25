package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources
import java.nio.file.Path

class MageTest : DiffTesterBase() {
    override val rootPath: Path = baseProjectPath.resolve("mage")
    override val model = ModelLoader(
        "mage",
        rootPath,
        collectSources(
            rootPath,
            rootPath.resolve("Mage.Server.Plugins")
        )
    )
}