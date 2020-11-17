package sibwaf.kawa

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spoon.Launcher
import spoon.SpoonAPI
import spoon.reflect.CtModel
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.system.measureTimeMillis

class ModelLoader(val name: String, private val sourcePaths: Iterable<File>) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ModelLoader::class.java)
    }

    constructor(name: String, vararg sourcePaths: File) : this(name, sourcePaths.asIterable())

    constructor(name: String, vararg sourcePaths: String) : this(name, sourcePaths.map { File(it) })

    val model by lazy {
        val file = File("models/$name.model")
        file.parentFile.mkdirs()

        val model: CtModel
        if (file.isFile) {
            log.info("Loading '$name' model...")
            val time = measureTimeMillis {
                model = file.inputStream().buffered().use {
                    ObjectInputStream(it).readObject()
                } as CtModel
            }

            log.info("Loaded model in %.2f seconds".format(time.toDouble() / 1000))
        } else {
            log.info("Building '$name' model...")
            val time = measureTimeMillis {
                val api = Launcher() as SpoonAPI
                for (path in sourcePaths) {
                    api.addInputResource(path.absolutePath)
                }
                model = api.buildModel()

                file.outputStream().buffered().use {
                    ObjectOutputStream(it).writeObject(model)
                }
            }

            log.info("Built and saved model in %.2f seconds".format(time.toDouble() / 1000))
        }

        model
    }
}