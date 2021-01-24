package sibwaf.kawa

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spoon.Launcher
import spoon.SpoonAPI
import spoon.reflect.CtModel
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Path
import kotlin.system.measureTimeMillis

data class SerializedModel(
    val root: String,
    val model: CtModel
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

class ModelLoader(val name: String, private val root: Path, private val sourcePaths: Iterable<File>) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ModelLoader::class.java)
    }

    constructor(name: String, root: Path, vararg sourcePaths: File) : this(name, root, sourcePaths.asIterable())

    constructor(name: String, root: Path, vararg sourcePaths: String) : this(name, root, sourcePaths.map { File(it) })

    init {
        if (sourcePaths.any { !it.toPath().startsWith(root) }) {
            throw IllegalArgumentException("All source paths must be descendants of the project root")
        }
    }

    val model by lazy {
        val file = File("models/$name.model")
        file.parentFile.mkdirs()

        var model: SerializedModel? = null
        if (file.isFile) {
            try {
                log.info("Loading '$name' model...")
                val time = measureTimeMillis {
                    model = file.inputStream().buffered().use {
                        ObjectInputStream(it).readObject()
                    } as SerializedModel
                }

                log.info("Loaded model in %.2f seconds".format(time.toDouble() / 1000))
            } catch (ignored: Exception) {
                log.warn("Failed to load cached model")
            }
        }

        if (model == null) {
            log.info("Building '$name' model...")
            val time = measureTimeMillis {
                val api = Launcher() as SpoonAPI
                for (path in sourcePaths) {
                    api.addInputResource(path.absolutePath)
                }
                model = SerializedModel(
                    root.toAbsolutePath().toString(),
                    api.buildModel()
                )

                file.outputStream().buffered().use {
                    ObjectOutputStream(it).writeObject(model)
                }
            }

            log.info("Built and saved model in %.2f seconds".format(time.toDouble() / 1000))
        }

        model!!
    }
}