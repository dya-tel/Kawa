package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader
import sibwaf.kawa.collectSources

class AsyncHttpClientTest : DiffTesterBase() {
    override val model = ModelLoader(
        "async-http-client",
        collectSources(
            baseProjectPath.resolve("async-http-client-master"),
            baseProjectPath.resolve("async-http-client-master").resolve("extras")
        )
    )
}