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
        if (getPluginVersion() == kannotatorSettings.getDismissedInVersion()) {
            return null
        }

        if (CompilerManager.getInstance(project)!!.isExcludedFromCompilation(file)) {
            return null
        }

        val panel = EditorNotificationPanel()
        val message = if (kannotatorSettings.getDismissedInVersion() == null) {
            "Do you want to automatically annotate libraries in your project with kannotator plugin?"
        }
        else {
            "Do you want to automatically annotate libraries in your project with updated kannotator plugin?"
        }

        panel.setText(message)
        panel.createActionLabel("Annotate", Runnable {
            AnnotateJarAction().annotateJars(project)
            dismissNotification(kannotatorSettings)
        })
        panel.createActionLabel("Dismiss", Runnable {
            dismissNotification(kannotatorSettings)
        })

        return panel
    }

    override fun getKey() : Key<EditorNotificationPanel> = KEY

    private fun dismissNotification(settings: KannotatorSettings) {
        settings.setDismissedInVersion(getPluginVersion());
        EditorNotifications.getInstance(project)!!.updateAllNotifications()
    }

    public fun getPluginVersion() : String {
        val pluginDescriptor = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kannotator"));
        return pluginDescriptor!!.getVersion()!!;
    }
}