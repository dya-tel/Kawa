package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources

class HibernateTest : DiffTesterBase() {
    override val model = ModelLoader(
            "hibernate",
            collectSources(baseProjectPath.resolve("hibernate-orm-master"))
    )
}