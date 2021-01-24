package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources
import java.nio.file.Path

class HibernateTest : DiffTesterBase() {
    override val rootPath: Path = baseProjectPath.resolve("hibernate-orm-master")
    override val model = ModelLoader(
        "hibernate",
        rootPath,
        collectSources(rootPath)
    )
}