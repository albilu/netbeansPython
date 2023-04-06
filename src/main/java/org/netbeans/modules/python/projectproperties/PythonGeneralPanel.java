package org.netbeans.modules.python.projectproperties;

import com.electronwill.nightconfig.core.file.FileConfig;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Triplet;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonProjectStateHandler;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.options.PythonPlatformManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Pair;

/**
 *
 * @author albilu
 */
public class PythonGeneralPanel extends javax.swing.JPanel implements DocumentListener {

    private static final long serialVersionUID = 1L;
    PythonProject project;

    /**
     * Creates new form PythonGeneralPanel
     */
    public PythonGeneralPanel(PythonProject project) {
        initComponents();
        this.project = project;
        pythonComboBox.setRenderer(new PythonPlatformCellRenderer());
        projectNameTextField.getDocument().addDocumentListener(this);
        versionTextField.getDocument().addDocumentListener(this);
        descriptionTextArea.getDocument().addDocumentListener(this);
        loadProperties();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectNameLabel = new javax.swing.JLabel();
        versionLabel = new javax.swing.JLabel();
        pythonLabel = new javax.swing.JLabel();
        descriptionLabel = new javax.swing.JLabel();
        projectNameTextField = new javax.swing.JTextField();
        versionTextField = new javax.swing.JTextField();
        pythonComboBox = new javax.swing.JComboBox<>();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(projectNameLabel, org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.projectNameLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(versionLabel, org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.versionLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(pythonLabel, org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.pythonLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(descriptionLabel, org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.descriptionLabel.text")); // NOI18N

        projectNameTextField.setText(org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.projectNameTextField.text")); // NOI18N

        versionTextField.setText(org.openide.util.NbBundle.getMessage(PythonGeneralPanel.class, "PythonGeneralPanel.versionTextField.text")); // NOI18N

        pythonComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pythonComboBoxActionPerformed(evt);
            }
        });

        descriptionTextArea.setColumns(20);
        descriptionTextArea.setRows(3);
        descriptionScrollPane.setViewportView(descriptionTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameLabel)
                    .addComponent(versionLabel)
                    .addComponent(pythonLabel)
                    .addComponent(descriptionLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(projectNameTextField)
                    .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                    .addComponent(pythonComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(versionTextField))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(projectNameLabel)
                    .addComponent(projectNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(versionLabel)
                    .addComponent(versionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pythonLabel)
                    .addComponent(pythonComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descriptionLabel)
                    .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void pythonComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pythonComboBoxActionPerformed
        markChange();
    }//GEN-LAST:event_pythonComboBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel descriptionLabel;
    private javax.swing.JScrollPane descriptionScrollPane;
    private static javax.swing.JTextArea descriptionTextArea;
    private javax.swing.JLabel projectNameLabel;
    private static javax.swing.JTextField projectNameTextField;
    private static javax.swing.JComboBox<Pair> pythonComboBox;
    private javax.swing.JLabel pythonLabel;
    private javax.swing.JLabel versionLabel;
    private static javax.swing.JTextField versionTextField;
    // End of variables declaration//GEN-END:variables

    private void loadProperties() {
        try {
            try (FileConfig conf = FileConfig.of(FileUtil.toFile(project.getProjectDirectory()
                    .getFileObject("pyproject.toml")))) {
                conf.load();
                boolean isPoetry = PythonUtility.isPoetry(project);
                projectNameTextField.setText(conf.getOrElse(isPoetry ? "tool.poetry.name" : "project.name", project
                        .getProjectDirectory().getName()));
                versionTextField.setText(conf.get(isPoetry ? "tool.poetry.version" : "project.version"));
                descriptionTextArea.setText(conf.get(isPoetry ? "tool.poetry.description" : "project.description"));
                FileObject projectDirectory = project.getProjectDirectory();
                FileObject fileObjectW = projectDirectory
                        .getFileObject(".venv\\Scripts\\python", "exe");
                FileObject fileObjectP = projectDirectory
                        .getFileObject(".venv/bin/python");
                HashSet<Pair> pyPlat = new HashSet();
                if (fileObjectW != null) {
//                    pythonComboBox.addItem(Pair.of("(.venv) " + PythonUtility
//                            .getVersion(fileObjectW.getPath()), fileObjectW.getPath()));
                    pyPlat.add(Pair.of("(.venv) " + PythonUtility
                            .getVersion(Paths.get(fileObjectW.getPath()).toString()), Paths.get(fileObjectW.getPath())
                            .toString()));
                } else if (fileObjectP != null) {
//                    pythonComboBox.addItem(Pair.of("(.venv) " + PythonUtility
//                            .getVersion(fileObjectP.getPath()), fileObjectP.getPath()));
                    pyPlat.add(Pair.of("(.venv) " + PythonUtility
                            .getVersion(Paths.get(fileObjectP.getPath()).toString()),
                            Paths.get(fileObjectP.getPath()).toString()));
                } else if (isPoetry) {
                    String lspPythonExe = PythonUtility.getLspPythonExe();
                    String poetryPythonPath = PythonUtility.getPoetryPythonPath(lspPythonExe, project.getProjectDirectory());
                    if (!poetryPythonPath.equals(lspPythonExe)) {
                        pyPlat.add(Pair.of(PythonUtility.getVersion(poetryPythonPath), poetryPythonPath));
                    }

                }
                Properties prop = PythonUtility.getProperties(project);
                Object python_path = prop.get("nbproject.python_path");
                if (python_path != null && !python_path.toString().isEmpty()) {
//                    pythonComboBox.addItem(Pair.of(PythonUtility
//                            .getVersion(python_path.toString()), python_path.toString()));
                    pyPlat.add(Pair.of(PythonUtility
                            .getVersion(python_path.toString()), python_path.toString()));
                }
                for (Pair pair : pyPlat) {
                    pythonComboBox.addItem(pair);
                }
                for (Triplet<String, String, Boolean> pythonExe : PythonPlatformManager.getPythonExes()) {
                    String exe = pythonExe.getValue0();
                    Pair<String, String> of = Pair.of(pythonExe.getValue1(), exe);
                    if (exe.equals(python_path)) {
                        pythonComboBox.setSelectedItem(of);
                        continue;
                    }
                    pythonComboBox.addItem(of);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void setProperties(PythonProject project) {
        Object selectedItem = pythonComboBox.getSelectedItem();
        try {
            File toFile = FileUtil.toFile(project.getProjectDirectory()
                    .getFileObject("pyproject.toml"));
//        try (FileConfig conf = FileConfig.of(toFile)) {
            String projectName = projectNameTextField.getText();
            String version = versionTextField.getText();
            String descr = descriptionTextArea.getText();
            String py = selectedItem != null
                    ? ">=" + StringUtils.remove(((Pair) selectedItem).first().toString(),
                            "Python ").replace("(.venv) ", "") : "";
//            conf.load();
//            conf.set("project.name", projectName);
//            conf.set("project.version", version);
//            conf.set("project.description", descr);
//            conf.set("project.requires-python", py);
//            conf.save();
            //FIXME-EXTERNAL This is a workaround until https://github.com/TheElectronWill/night-config/issues/123
            //is fixed
            File tomlHandler = PythonUtility.TOML_HANDLER;
            String[] cmd = {
                PythonUtility.getLspPythonExe(),
                tomlHandler.toPath().toString(),
                toFile.toPath().toString(),
                projectName,
                version,
                descr,
                py,
                PythonUtility.getProjectType(toFile)
            };

            PythonUtility.processExecutor(cmd, "Update Properties");

            Properties prop = PythonUtility.getProperties(project);
            prop.setProperty("nbproject.python_path", selectedItem != null
                    ? ((Pair) selectedItem).second().toString() : "");
            prop.store(new FileWriter(FileUtil.toFile(project.getProjectDirectory()
                    .getFileObject("nbproject/project.properties"))), null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent de) {
        markChange();
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        markChange();
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
        markChange();
    }

    void markChange() {
        project.getLookup().lookup(PythonProjectStateHandler.class).mark();
    }
}
