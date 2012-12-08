package org.jetbrains.kannotator.plugin.persistentState;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = KannotatorSettings.COMPONENT_NAME,
       storages = {
                @Storage(file = StoragePathMacros.PROJECT_FILE),
                @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/kannotator.xml", scheme = StorageScheme.DIRECTORY_BASED)
       }
)
public class KannotatorSettings implements PersistentStateComponent<KannotatorSettings> {
    @NonNls
    public static final String COMPONENT_NAME = "KannotatorSettings";

    private boolean disableCheckUntilNextVersion;

    @Nullable
    @Override
    public KannotatorSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull KannotatorSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Nullable
    public static KannotatorSettings getOptions(Project project) {
        return ServiceManager.getService(project, KannotatorSettings.class).getState();
    }

    public boolean isDisableCheckUntilNextVersion() {
        return disableCheckUntilNextVersion;
    }

    public void setDisableCheckUntilNextVersion(boolean disableCheckUntilNextVersion) {
        this.disableCheckUntilNextVersion = disableCheckUntilNextVersion;
    }
}
