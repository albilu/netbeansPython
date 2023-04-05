package org.netbeans.modules.python.projectproperties;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonProjectStateHandler;
import org.netbeans.modules.python.PythonUtility;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonPdocPanel extends javax.swing.JPanel implements DocumentListener {

    private static final long serialVersionUID = 1L;

    PythonProject project;

    /**
     * Creates new form PythonDocPanel
     */
    public PythonPdocPanel(PythonProject project) {
        initComponents();
        this.project = project;
        pdocParamsTextField.getDocument().addDocumentListener(this);
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

        pdocParamsLabel = new javax.swing.JLabel();
        pdocParamsTextField = new javax.swing.JTextField();

        setBorder(javax.swing.BorderFactory.createTitledBorder(null, org.openide.util.NbBundle.getMessage(PythonPdocPanel.class, "PythonPdocPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(pdocParamsLabel, org.openide.util.NbBundle.getMessage(PythonPdocPanel.class, "PythonPdocPanel.pdocParamsLabel.text")); // NOI18N

        pdocParamsTextField.setText(org.openide.util.NbBundle.getMessage(PythonPdocPanel.class, "PythonPdocPanel.pdocParamsTextField.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pdocParamsLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pdocParamsTextField)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pdocParamsLabel)
                    .addComponent(pdocParamsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel pdocParamsLabel;
    public static javax.swing.JTextField pdocParamsTextField;
    // End of variables declaration//GEN-END:variables

    private void loadProperties() {
        try {
            Properties conf = PythonUtility.getConf(project);
            pdocParamsTextField.setText(conf.getProperty("nbproject.pdoc.params",
                    "-o docs"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void setProperties(PythonProject project) {
        try {
            Properties conf = PythonUtility.getConf(project);
            conf.setProperty("nbproject.pdoc.params", pdocParamsTextField.getText());
            conf.store(new FileWriter(FileUtil.toFile(project.getProjectDirectory()
                    .getFileObject("nbproject/project.properties"))), null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    void markChange() {
        project.getLookup().lookup(PythonProjectStateHandler.class).mark();
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

}
