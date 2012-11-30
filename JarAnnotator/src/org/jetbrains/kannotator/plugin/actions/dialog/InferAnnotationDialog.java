package org.jetbrains.kannotator.plugin.actions.dialog;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;

public class InferAnnotationDialog extends DialogWrapper {
    JPanel contentPanel;
    JCheckBox nullabilityCheckBox;
    JCheckBox kotlinSignaturesCheckBox;
    TextFieldWithBrowseButton outputDirectory;
    JBScrollPane jarsTreeScrollPane;
    JLabel outputDirectoryLabel;
    JLabel jarsTreeLabel;

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
                RefactoringBundle.message("select.target.directory"),
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
        nullabilityCheckBox.setSelected(true);
        kotlinSignaturesCheckBox.setSelected(true);
        outputDirectory.getTextField().setText(new File(FileUtil.toSystemDependentName(project.getBaseDir().getPath()), "annotations").getAbsolutePath());
    }

    @Nullable
    public String getConfiguredOutputPath() {
        String outputPath = FileUtil.toSystemIndependentName(outputDirectory.getText().trim());
        if (outputPath.length() == 0) {
            outputPath = null;
        }
        return outputPath;
    }

    public boolean shouldInferNullabilityAnnotations() {
        return nullabilityCheckBox.isSelected();
    }

    public boolean shouldInferKotlinAnnotations() {
        return kotlinSignaturesCheckBox.isSelected();
    }

    protected void updateControls() {
        boolean someAnnotationTypeSelected = shouldInferNullabilityAnnotations() || shouldInferKotlinAnnotations();
        setOKActionEnabled(getConfiguredOutputPath() != null && someAnnotationTypeSelected);
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
