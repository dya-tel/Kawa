package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import java.nio.file.Path

class EventBusTest : DiffTesterBase() {
    override val rootPath: Path = baseProjectPath.resolve("EventBus")
    override val model = ModelLoader(
        "event-bus",
        rootPath,
        rootPath.resolve("EventBus").resolve("src").toFile()
    )
}