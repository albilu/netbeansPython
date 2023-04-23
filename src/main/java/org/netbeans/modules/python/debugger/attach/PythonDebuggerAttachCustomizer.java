package org.netbeans.modules.python.debugger.attach;

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.debugger.Properties;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.debugger.attach.Processes.ProcessInfo;
import org.netbeans.spi.debugger.ui.Controller;
import static org.netbeans.spi.debugger.ui.Controller.PROP_VALID;
import org.netbeans.spi.debugger.ui.PersistentController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonDebuggerAttachCustomizer extends javax.swing.JPanel {

    private final ConnectController controller;
    private final ValidityDocumentListener validityDocumentListener = new ValidityDocumentListener();
    private final RequestProcessor RP = new RequestProcessor(PythonDebuggerAttachCustomizer.class.getName(), 2);
    private ProcessInfo processInfo = null;

    /**
     * Creates new form PythonDebuggerAttachCustomizer
     */
    public PythonDebuggerAttachCustomizer() {
        controller = new ConnectController();
        initComponents();
        initNIFile();
        initProcesses();
    }

    private void initNIFile() {
        RP.post(() -> {
            FileObject currentFO = Utilities.actionsGlobalContext().lookup(FileObject.class);
            if (currentFO != null) {
                File currentFile = FileUtil.toFile(currentFO);
                String path;
                if (currentFile != null && currentFile.canExecute()) {
                    path = currentFile.getAbsolutePath();
                } else {
                    Project project = FileOwnerQuery.getOwner(currentFO);
                    if (project != null) {
                        currentFO = project.getProjectDirectory();
                        currentFile = FileUtil.toFile(currentFO);
                        path = currentFile.getAbsolutePath();
                    } else {
                        path = null;
                    }
                }
            }
        });
    }

    private void initProcesses() {
        processTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final int spacing = 8;
        processTable.setIntercellSpacing(new Dimension(spacing, 0));
        Mnemonics.setLocalizedText(attachLabel, org.openide.util.NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.attachLabel.text", 0l)); // NOI18N
        RP.post(() -> {
            List<ProcessInfo> processes = Processes.getAllProcesses();
            int size = processes.size();
            Object[][] processValues = new Object[size][];
            for (int i = 0; i < size; i++) {
                ProcessInfo info = processes.get(i);
                processValues[i] = new Object[]{
                    info.getPid(),
                    info.getCommand()
                };
            }
            SwingUtilities.invokeLater(() -> {
                processTable.setModel(new javax.swing.table.DefaultTableModel(
                        processValues,
                        new String[]{
                            NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.processPID.text"), // NOI18N
                            NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.processCommand.text"), // NOI18N
                        }
                ) {
                    Class<?>[] types = new Class<?>[]{
                        Long.class, String.class
                    };

                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return types[columnIndex];
                    }

                    @Override
                    public boolean isCellEditable(int rowIndex, int columnIndex) {
                        return false;
                    }
                });
                if (!processes.isEmpty()) {
                    int width = processTable.getGraphics().getFontMetrics().stringWidth(Long.toString(processes.get(0).getPid()));
                    width += 2 * spacing;
                    processTable.getColumnModel().getColumn(0).setPreferredWidth(width);
                    processTable.getColumnModel().getColumn(0).setMaxWidth(width);
                    processTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
                        int index = processTable.getSelectedRow();
                        if (index < 0) {
                            Mnemonics.setLocalizedText(attachLabel, NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.attachLabel.text", 0l)); // NOI18N
                            processInfo = null;
                        } else {
                            ProcessInfo info = processes.get(index);
                            Mnemonics.setLocalizedText(attachLabel, NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.attachLabel.text", info.getPid())); // NOI18N
                            processInfo = info;
                        }
                    });
                }
            });
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dbgLabel = new javax.swing.JLabel();
        dbgTextField = new javax.swing.JTextField();
        attachLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        processTable = new javax.swing.JTable();

        dbgLabel.setLabelFor(dbgTextField);
        org.openide.awt.Mnemonics.setLocalizedText(dbgLabel, org.openide.util.NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.dbgLabel.text")); // NOI18N

        dbgTextField.setText(org.openide.util.NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.dbgTextField.text")); // NOI18N

        attachLabel.setLabelFor(processTable);
        org.openide.awt.Mnemonics.setLocalizedText(attachLabel, org.openide.util.NbBundle.getMessage(PythonDebuggerAttachCustomizer.class, "PythonDebuggerAttachCustomizer.attachLabel.text")); // NOI18N

        jScrollPane1.setViewportView(processTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(dbgLabel)
                        .addGap(10, 10, 10)
                        .addComponent(dbgTextField)
                        .addGap(85, 85, 85))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(attachLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dbgLabel)
                    .addComponent(dbgTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(attachLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel attachLabel;
    private javax.swing.JLabel dbgLabel;
    private javax.swing.JTextField dbgTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable processTable;
    // End of variables declaration//GEN-END:variables

    RequestProcessor.Task validationTask = new RequestProcessor(PythonDebuggerAttachCustomizer.class).create(new FileValidationTask());

    @NbBundle.Messages({"MSG_NoFile=Native Imige File is missing."})
    private void checkValid() {
        assert SwingUtilities.isEventDispatchThread() : "Called outside of AWT.";
//        if (fileTextField.getText().isEmpty()) {
//            controller.setInformationMessage(Bundle.MSG_NoFile());
//            controller.setValid(false);
//            return;
//        }
        validationTask.schedule(200);
    }

    private class FileValidationTask implements Runnable {

        @Override
        public void run() {
        }
    }

    private class ValidityDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            checkValid();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            checkValid();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            checkValid();
        }
    }

    Controller getController() {
        return controller;
    }

    public class ConnectController implements PersistentController {

        private static final String NI_ATTACH_PROPERTIES = "native_image_attach_settings";
        private static final String PROP_DBG = "debugger";

        private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
        private boolean valid = true;

        @Override
        public String getDisplayName() {
//            return dbgTextField.getText() + " " + new File(fileTextField.getText()).getName();
            return "Python";
        }

        @Override
        public boolean load(Properties props) {
            assert !SwingUtilities.isEventDispatchThread();
            final Properties attachProps = props.getProperties(NI_ATTACH_PROPERTIES);
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        dbgTextField.setText(attachProps.getString(PROP_DBG, "PDB"));
                    }
                });
            } catch (InterruptedException | InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
            return true;
        }

        @Override
        public void save(Properties props) {
            final Properties attachProps = props.getProperties(NI_ATTACH_PROPERTIES);
            if (SwingUtilities.isEventDispatchThread()) {
                saveToProps(attachProps);
            } else {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            saveToProps(attachProps);

                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        private void saveToProps(Properties attachProps) {
            attachProps.setString(PROP_DBG, dbgTextField.getText());
        }

        @Override
        public boolean ok() {
            String debuggerCommand = dbgTextField.getText();
            ProcessInfo attach2Process = processInfo;
            RP.post(() -> {
                //TODO Run Python debugger with process ID
//                NIDebugRunner.start(file, startParams, null, null);
//                PythonDebugger.startDebugger();
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("Not supported at the moment", NotifyDescriptor.INFORMATION_MESSAGE));
            });
            return true;
        }

        @Override
        public boolean cancel() {
            return true;
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        void setValid(boolean valid) {
            this.valid = valid;
            firePropertyChange(PROP_VALID, !valid, valid);
        }

        void setErrorMessage(String msg) {
            firePropertyChange(NotifyDescriptor.PROP_ERROR_NOTIFICATION, null, msg);
        }

        void setInformationMessage(String msg) {
            firePropertyChange(NotifyDescriptor.PROP_INFO_NOTIFICATION, null, msg);
        }

        private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            pcs.firePropertyChange(propertyName, oldValue, newValue);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
            pcs.addPropertyChangeListener(l);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
            pcs.removePropertyChangeListener(l);
        }

    }

}
