package org.jetbrains.kannotator.plugin.ideaUtils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.util.ActionRunner
import com.intellij.util.ActionRunner.InterruptibleRunnableWithResult
import com.intellij.util.ActionRunner.InterruptibleRunnable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import java.io.File

public fun runWriteAction(block: () -> Unit) {
    ApplicationManager.getApplication()!!.runWriteAction(object: Runnable {
        override fun run() {
            block()
        }
    })
}

public fun <T> runComputableWriteAction(block: () -> T): T {
    return ApplicationManager.getApplication()!!.runWriteAction(object: Computable<T> {
        override fun compute(): T {
            return block()
        }
    })
}

public fun <T> runComputableInsideWriteAction(block: () -> T) : T {
    return ActionRunner.runInsideWriteAction(object: InterruptibleRunnableWithResult<T> {
        override fun run(): T {
            return block()
        }
    })
}

public fun runInsideWriteAction(block: () -> Unit) {
    ActionRunner.runInsideWriteAction(object: InterruptibleRunnable {
        override fun run() {
            block()
        }
    })
}

public fun runInsideReadAction(block: () -> Unit) {
    ActionRunner.runInsideReadAction(object: InterruptibleRunnable {
        override fun run() {
            block()
        }
    })
}

public fun VirtualFile.toIO(): File = VfsUtilCore.virtualToIoFile(this)

public fun Set<VirtualFile>.toIO(): Set<File> = map { it.toIO() }.toSet()