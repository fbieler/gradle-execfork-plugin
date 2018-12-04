package com.github.psxpaul.task

import org.gradle.api.internal.file.FileResolver
import org.gradle.process.ProcessForkOptions
import org.gradle.process.internal.DefaultJavaForkOptions
import javax.inject.Inject

/**
 * A task that will run a command in a separate process, optionally
 * writing stdout and stderr to disk, and waiting for a specified
 * port to be open.
 *
 * @see AbstractExecFork
 * @see ProcessForkOptions for all available configuration options
 */
open class ExecFork @Inject constructor(fileResolver: FileResolver) : AbstractExecFork(),
        ProcessForkOptions by DefaultJavaForkOptions(fileResolver) {

    override fun getProcessArgs(): List<String>? {
        val processArgs = mutableListOf<String>()
        processArgs.add(executable!!)
        processArgs.addAll(args)
        return processArgs
    }

}