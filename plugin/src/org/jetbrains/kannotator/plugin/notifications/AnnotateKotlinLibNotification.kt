package org.jetbrains.kannotator.plugin.notifications

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import org.jetbrains.kannotator.plugin.actions.AnnotateJarAction
import org.jetbrains.kannotator.plugin.persistentState.KannotatorSettings

private val KEY = Key.create<EditorNotificationPanel>("annotate.kotlin.lib");

public class AnnotateKotlinLibNotification(val project: Project): EditorNotifications.Provider<EditorNotificationPanel>() {
    override fun createNotificationPanel(file : VirtualFile, fileEditor : FileEditor) : EditorNotificationPanel? {
        if (!PluginManager.isPluginInstalled(PluginId.getId("org.jetbrains.kotlin"))) {
            return null
        }

        if (file.getFileType().getName() != "Kotlin") {
            return null
        }

        val kannotatorSettings = KannotatorSettings.getOptions(project)!!
        if (kannotatorSettings.isDisableCheckUntilNextVersion()) {
            return null
        }

        if (CompilerManager.getInstance(project)!!.isExcludedFromCompilation(file)) {
            return null
        }

        val panel = EditorNotificationPanel()
        panel.setText("Do you want to automatically annotate libraries in your project with kannotator plugin?")
        panel.createActionLabel("Annotate", runnable {
            AnnotateJarAction().annotateJars(project)
            dismissNotification(kannotatorSettings)
        })
        panel.createActionLabel("Dismiss", runnable {
            dismissNotification(kannotatorSettings)
        })

        return panel
    }

    override fun getKey() : Key<EditorNotificationPanel> = KEY

    private fun dismissNotification(settings: KannotatorSettings) {
        settings.setDisableCheckUntilNextVersion(true);
        EditorNotifications.getInstance(project)!!.updateAllNotifications()
    }
}