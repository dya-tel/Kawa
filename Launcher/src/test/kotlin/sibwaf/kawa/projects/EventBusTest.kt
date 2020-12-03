package sibwaf.kawa.projects

import sibwaf.kawa.DiffTesterBase
import sibwaf.kawa.ModelLoader

class EventBusTest : DiffTesterBase() {
    override val model = ModelLoader(
        "event-bus",
        "EventBus-master/EventBus/src"
    )
}