package com.github.psxpaul.task

import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.internal.jvm.Jvm
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.DefaultJavaForkOptions
import javax.inject.Inject

/**
 * A task that will run a java class in a separate process, optionally
 * writing stdout and stderr to disk, and waiting for a specified
 * port to be open.
 *
 * @see AbstractExecFork
 *
 * @param classpath the classpath to call java with
 * @param main the fully qualified name of the class to execute (e.g. 'com.foo.bar.MainExecutable')
 */
open class JavaExecFork @Inject constructor(fileResolver: FileResolver) : AbstractExecFork(),
        JavaForkOptions by DefaultJavaForkOptions(fileResolver) {

    @InputFiles
    var classpath: FileCollection? = null

    @Input
    var main: String? = null

    override fun getProcessArgs(): List<String>? {
        val processArgs = mutableListOf<String>()
        processArgs.add(Jvm.current().javaExecutable.absolutePath)
        processArgs.add("-cp")
        processArgs.add((bootstrapClasspath + classpath!!).asPath)
        processArgs.addAll(allJvmArgs)
        processArgs.add(main!!)
        processArgs.addAll(args)
        return processArgs
    }
}
