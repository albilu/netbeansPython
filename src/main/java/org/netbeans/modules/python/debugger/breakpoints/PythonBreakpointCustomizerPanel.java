package org.netbeans.modules.python.debugger.breakpoints;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Properties;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
import org.netbeans.spi.debugger.ui.Controller;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.HelpCtx;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonBreakpointCustomizerPanel extends javax.swing.JPanel
        implements ControllerProvider, HelpCtx.Provider {

    private static final Logger LOG = Logger.getLogger(PythonBreakpointCustomizerPanel.class.getName());

    private static final int MAX_SAVED_CONDITIONS = 10;
    private static final RequestProcessor RP = new RequestProcessor(PythonBreakpointCustomizerPanel.class);
    private static final String[] PYTHON_MIME_TYPES = new String[]{PythonUtility.PYTHON_MIME_TYPE};
    private static final Set<String> PYTHON_MIME_TYPES_SET = new HashSet<>(Arrays.asList(PYTHON_MIME_TYPES));

    private final Controller controller;
    private final PythonBreakpoint lb;
    private boolean createBreakpoint;

    private static PythonBreakpoint createBreakpoint() {
        Line line = getCurrentLine();
        if (line == null) {
            return null;
        }
        return PythonBreakpoint.create(line);
    }

    private static Line getCurrentLine() {
        FileObject fo = EditorContextDispatcher.getDefault().getCurrentFile();
        //System.out.println("n = "+n+", FO = "+fo+" => is ANT = "+isAntFile(fo));
        if (!isPythonFile(fo)) {
            return null;
        }
        return EditorContextDispatcher.getDefault().getCurrentLine();
    }

    private static boolean isPythonFile(FileObject fo) {
        if (fo == null) {
            return false;
        } else {
            return PYTHON_MIME_TYPES_SET.contains(fo.getMIMEType());
        }
    }

    /**
     * Creates new form LineBreakpointCustomizer
     */
    public PythonBreakpointCustomizerPanel() {
        this(createBreakpoint());
        createBreakpoint = true;
    }

    /**
     * Creates new form LineBreakpointCustomizer
     */
    public PythonBreakpointCustomizerPanel(PythonBreakpoint lb) {
        this.lb = lb;
        initComponents();
        controller = createController();
        if (lb != null) {
            Line line = lb.getLine();
            FileObject fo = line.getLookup().lookup(FileObject.class);
            if (fo != null) {
                File file = FileUtil.toFile(fo);
                if (file != null) {
                    fileTextField.setText(file.getAbsolutePath());
                } else {
                    fileTextField.setText(fo.toURL().toExternalForm());
                }
            }
            lineTextField.setText(Integer.toString(line.getLineNumber() + 1));
            Object[] conditions = getSavedConditions();
            conditionComboBox.setModel(new DefaultComboBoxModel(conditions));
            String condition = lb.getCondition();
            if (condition != null && !condition.isEmpty()) {
                conditionCheckBox.setSelected(true);
                conditionComboBox.setEnabled(true);
                conditionComboBox.getEditor().setItem(condition);
            } else {
                conditionCheckBox.setSelected(false);
                conditionComboBox.setEnabled(false);
            }
        }
    }

    private static Object[] getSavedConditions() {
        return Properties.getDefault().getProperties("debugger.python").
                getArray("BPConditions", new Object[0]);
    }

    private static void saveCondition(String condition) {
        Object[] savedConditions = getSavedConditions();
        Object[] conditions = null;
        boolean containsCondition = false;
        for (int i = 0; i < savedConditions.length; i++) {
            Object c = savedConditions[i];
            if (condition.equals(c)) {
                containsCondition = true;
                conditions = savedConditions;
                if (i > 0) {
                    System.arraycopy(conditions, 0, conditions, 1, i);
                    conditions[0] = condition;
                }
                break;
            }
        }
        if (!containsCondition) {
            if (savedConditions.length < MAX_SAVED_CONDITIONS) {
                conditions = new Object[savedConditions.length + 1];
                conditions[0] = condition;
                System.arraycopy(savedConditions, 0, conditions, 1, savedConditions.length);
            } else {
                conditions = savedConditions;
                System.arraycopy(conditions, 0, conditions, 1, conditions.length - 1);
                conditions[0] = condition;
            }
        }
        Properties.getDefault().getProperties("debugger.python").
                setArray("BPConditions", conditions);
    }

    protected Controller createController() {
        return new CustomizerController();
    }

    @Override
    public Controller getController() {
        return controller;
    }

    protected RequestProcessor getUpdateRP() {
        return RP;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileLabel = new javax.swing.JLabel();
        fileTextField = new javax.swing.JTextField();
        lineLabel = new javax.swing.JLabel();
        lineTextField = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        conditionCheckBox = new javax.swing.JCheckBox();
        conditionComboBox = new javax.swing.JComboBox();

        fileLabel.setLabelFor(fileTextField);
        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.fileLabel.text")); // NOI18N

        fileTextField.setToolTipText(org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.fileTextField.toolTipText")); // NOI18N

        lineLabel.setLabelFor(lineTextField);
        org.openide.awt.Mnemonics.setLocalizedText(lineLabel, org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.lineLabel.text")); // NOI18N

        lineTextField.setToolTipText(org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.lineTextField.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(conditionCheckBox, org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.conditionCheckBox.text")); // NOI18N
        conditionCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                conditionCheckBoxActionPerformed(evt);
            }
        });

        conditionComboBox.setEditable(true);
        conditionComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(PythonBreakpointCustomizerPanel.class, "PythonBreakpointCustomizerPanel.conditionComboBox.toolTipText")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(conditionCheckBox)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(conditionComboBox, 0, 330, Short.MAX_VALUE))
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lineLabel)
                            .addComponent(fileLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fileTextField)
                            .addComponent(lineTextField))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileLabel)
                    .addComponent(fileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lineLabel)
                    .addComponent(lineTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(conditionCheckBox)
                    .addComponent(conditionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void conditionCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_conditionCheckBoxActionPerformed
        conditionComboBox.setEnabled(conditionCheckBox.isSelected());
        if (conditionCheckBox.isSelected()) {
            conditionComboBox.requestFocusInWindow();
        }
    }//GEN-LAST:event_conditionCheckBoxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox conditionCheckBox;
    private javax.swing.JComboBox conditionComboBox;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileTextField;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lineLabel;
    private javax.swing.JTextField lineTextField;
    // End of variables declaration//GEN-END:variables

    @Override
    public HelpCtx getHelpCtx() {
        return new org.openide.util.HelpCtx("NetbeansDebuggerLineBreakpointJavaScript"); // NOI18N
    }

    private static String toURL(String filePath) {
        URI uri = null;
        try {
            uri = URI.create(filePath);
        } catch (Exception ex) {
        }
        if (uri == null || !uri.isAbsolute()) {
            File f = new File(filePath);
            uri = Utilities.toURI(f);
        }
        try {
            return uri.toURL().toExternalForm();
        } catch (MalformedURLException ex) {
        }
        return filePath;
    }

    private class CustomizerController implements Controller {

        @Override
        public boolean ok() {
            PythonBreakpoint lb = PythonBreakpointCustomizerPanel.this.lb;
            String fileStr = toURL(fileTextField.getText());
            int lineNumber;
            try {
                lineNumber = Integer.parseInt(lineTextField.getText());
            } catch (NumberFormatException nfex) {
                return false;
            }
            lineNumber--;
            Line line = getLine(fileStr, lineNumber);
            if (line == null) {
                return false;
            }
            if (lb != null) {
                updateBreakpoint(lb, line);
            } else {
                lb = PythonBreakpoint.create(line);
            }
            setCondition(lb);
            if (createBreakpoint) {
                boolean managePdbBreakPoints = PythonDebuggerUtils.managePdbBreakPoints(lb, "add");
                if (managePdbBreakPoints) {
                    DebuggerManager.getDebuggerManager().addBreakpoint(lb);
                }
            }
            return true;
        }

        private void setCondition(PythonBreakpoint lb1) {
            String condition = null;
            if (conditionCheckBox.isSelected()) {
                condition = conditionComboBox.getSelectedItem().toString().trim();
            }
            if (condition != null && !condition.isEmpty()) {
                lb1.setCondition(condition);
                saveCondition(condition);
            } else {
                lb1.setCondition(null);
            }
        }

        private void updateBreakpoint(PythonBreakpoint lb, final Line line) {
            getUpdateRP().post(() -> {
                //                    lb.setLineHandler(JSUtils.createLineHandler(line));
                boolean managePdbBreak = PythonDebuggerUtils.managePdbBreakPoints(lb, "remove");
                if (managePdbBreak) {
                    DebuggerManager.getDebuggerManager().removeBreakpoint(lb);
                }
                PythonBreakpoint create = PythonBreakpoint.create(line);
                setCondition(create);
                boolean managePdbBreakPoints = PythonDebuggerUtils.managePdbBreakPoints(create, "add");
                if (managePdbBreakPoints) {
                    DebuggerManager.getDebuggerManager().addBreakpoint(create);

                }
            });
        }

        @Override
        public boolean cancel() {
            return true;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

    }

    public static Line getLine(final String filePath, final int lineNumber) {
        if (filePath == null || lineNumber < 0) {
            return null;
        }

        FileObject fileObject = null;
        URI uri = URI.create(filePath);
        if (uri.isAbsolute()) {
            URL url;
            try {
                url = uri.toURL();
            } catch (MalformedURLException muex) {
                // Issue 230657
                LOG.log(Level.INFO, "Cannot resolve " + filePath, muex); // NOI18N
                return null;
            }
            fileObject = URLMapper.findFileObject(url);
        }
        if (fileObject == null) {
            File file;
            if (filePath.startsWith("file:/")) {
                file = Utilities.toFile(uri);
            } else {
                file = new File(filePath);
            }
            fileObject = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        }
        if (fileObject == null) {
            LOG.log(Level.INFO, "Cannot resolve \"{0}\"", filePath);
            return null;
        }

        LineCookie lineCookie = getLineCookie(fileObject);
        if (lineCookie == null) {
            LOG.log(Level.INFO, "No line cookie for \"{0}\"", fileObject);
            return null;
        }
        try {
            return lineCookie.getLineSet().getCurrent(lineNumber);
        } catch (IndexOutOfBoundsException ioob) {
            List<? extends Line> lines = lineCookie.getLineSet().getLines();
            if (!lines.isEmpty()) {
                return lines.get(lines.size() - 1);
            } else {
                return null;
            }
        }
    }

    public static LineCookie getLineCookie(final FileObject fo) {
        LineCookie result = null;
        try {
            DataObject dataObject = DataObject.find(fo);
            if (dataObject != null) {
                result = dataObject.getLookup().lookup(LineCookie.class);
            }
        } catch (DataObjectNotFoundException e) {
        }
        return result;
    }
}
