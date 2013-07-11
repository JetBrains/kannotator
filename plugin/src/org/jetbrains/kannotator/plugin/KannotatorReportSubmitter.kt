package org.jetbrains.kannotator.plugin;

import com.intellij.diagnostic.ITNReporter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent

public class KannotatorReportSubmitter: ITNReporter() {
    public override fun showErrorInRelease(event: IdeaLoggingEvent?): Boolean {
        return true
    }
}
