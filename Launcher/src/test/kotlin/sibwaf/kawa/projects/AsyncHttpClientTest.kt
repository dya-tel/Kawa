package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources
import java.nio.file.Path

class AsyncHttpClientTest : DiffTesterBase() {
    override val rootPath: Path = baseProjectPath.resolve("async-http-client")
    override val model = ModelLoader(
        "async-http-client",
        rootPath,
        collectSources(
            rootPath,
            rootPath.resolve("extras")
        )
    )
}