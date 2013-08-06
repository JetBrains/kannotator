package org.jetbrains.kannotator.plugin.actions.dialog;

import com.intellij.ide.util.projectWizard.ProjectWizardUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.util.*;

public class InferAnnotationDialog extends DialogWrapper {
    JPanel contentPanel;
    JCheckBox nullabilityCheckBox;
    JCheckBox kotlinSignaturesCheckBox;
    TextFieldWithBrowseButton outputDirectory;
    JBScrollPane jarsTreeScrollPane;
    JLabel outputDirectoryLabel;
    JLabel jarsTreeLabel;
    JCheckBox addLibrariesRootAutomaticallyCheckbox;
    JCheckBox removeAllOtherAnnotationsRootsCheckbox;
    JCheckBox useCommonTreeCheckBox;

    // Not from gui designer
    LibraryCheckboxTree libraryTree;

    @NotNull
    private Project project;

    public InferAnnotationDialog(@NotNull Project project) {
        super(project);

        this.project = project;
        setDefaultValues();

        init();

        updateControls();
    }

    @Override
    protected void init() {
        setTitle("Annotate Jar Files");

        contentPanel.setPreferredSize(new Dimension(440, 500));

        outputDirectory.addBrowseFolderListener(
                "Select output directory",
                "Inferred annotation will be written to this folder",
                null, FileChooserDescriptorFactory.createSingleFolderDescriptor());

        outputDirectory.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            protected void textChanged(final DocumentEvent e) {
                updateControls();
            }
        });

        outputDirectoryLabel.setLabelFor(outputDirectory.getTextField());

        nullabilityCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateControls();
            }
        });

        kotlinSignaturesCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateControls();
            }
        });

        LibraryItemsTreeController libraryItemsTreeController = new LibraryItemsTreeController();
        libraryTree = new LibraryCheckboxTree(libraryItemsTreeController);
        libraryItemsTreeController.buildTree(libraryTree, project);

        jarsTreeScrollPane.setViewportView(libraryTree);

        jarsTreeLabel.setLabelFor(libraryTree);

        super.init();
    }

    protected void setDefaultValues() {
        outputDirectory.getTextField().setText(new File(FileUtil.toSystemDependentName(project.getBaseDir().getPath()), "annotations").getAbsolutePath());
    }

    @Nullable
    private String getConfiguringOutputPath() {
        String outputPath = FileUtil.toSystemIndependentName(outputDirectory.getText().trim());
        if (outputPath.length() == 0) {
            outputPath = null;
        }
        return outputPath;
    }

    @NotNull
    public String getConfiguredOutputPath() {
        String configuredOutputPath = getConfiguringOutputPath();
        if (configuredOutputPath == null) {
            throw new IllegalStateException("Output path wasn't properly configured");
        }

        return configuredOutputPath;
    }

    /**
     * Check whether we should create a separate directory for each annotated library or
     * output annotations to the same directory tree.
     * @return false if each library has its own branch, true otherwise
     */
    @NotNull
    public boolean useOneCommonTree()
    {
        return useCommonTreeCheckBox.isSelected();
    }

    @Override
    protected void doOKAction() {
        if (ProjectWizardUtil.createDirectoryIfNotExists("Output directory", getConfiguredOutputPath(), true)
                && notifyAboutNonEmptyOutput()) {
            super.doOKAction();
        }
    }

    protected boolean notifyAboutNonEmptyOutput() {
        final VirtualFile baseDir = ApplicationManager.getApplication().runWriteAction(new Computable<VirtualFile>() {
            public VirtualFile compute() {
                return LocalFileSystem.getInstance().refreshAndFindFileByPath(getConfiguredOutputPath());
            }
        });

        baseDir.refresh(false, true);

        if (baseDir.getChildren().length > 0) {
            int rc = Messages.showYesNoDialog(project,
                    "The directory '" + getConfiguredOutputPath() + "' is not empty.\n" +
                            "Inferrer will rewrite existing files in conflict situation. Do you want to proceed?",
                    "Output Directory Is Not Empty", Messages.getWarningIcon());
            return (rc == 0);
        }

        return true;
    }

    @KotlinSignature("fun getCheckedLibToJarFiles() : Map<Library, Set<VirtualFile>>")
    public Map<Library, Set<VirtualFile>> getCheckedLibToJarFiles() {
        //noinspection unchecked
        return (Map) libraryTree.getController().getCheckedLibToJarFiles();
    }

    public boolean shouldInferNullabilityAnnotations() {
        return nullabilityCheckBox.isSelected();
    }

    public boolean shouldInferKotlinAnnotations() {
        return kotlinSignaturesCheckBox.isSelected();
    }

    public boolean shouldAddAnnotationsRoots() {
        return addLibrariesRootAutomaticallyCheckbox.isSelected();
    }

    public boolean shouldRemoveAllOtherRoots() {
        return removeAllOtherAnnotationsRootsCheckbox.isSelected();
    }

    protected void updateControls() {
        boolean someAnnotationTypeSelected = shouldInferNullabilityAnnotations() || shouldInferKotlinAnnotations();
        setOKActionEnabled(getConfiguringOutputPath() != null && someAnnotationTypeSelected);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return libraryTree;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPanel;
    }
}