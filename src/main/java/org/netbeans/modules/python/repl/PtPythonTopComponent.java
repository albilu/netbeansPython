package org.netbeans.modules.python.repl;

import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.project.PythonProject;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.netbeans.modules.python.actions//PtPython//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "PtPythonTopComponent",
        iconBase = "org/netbeans/modules/python/python-2.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "org.netbeans.modules.python.actions.PtPythonTopComponent")
@ActionReference(path = "Menu/Window/Interactive Python Interpreters", position = 333)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PtPythonAction",
        preferredID = "PtPythonTopComponent"
)
@Messages({
    "CTL_PtPythonAction=PtPython",
    "CTL_PtPythonTopComponent=PtPython Shell",
    "CTL_PtPythonStart=Starting",
    "CTL_PtPythonOpening=Opening PtPython console",
    "HINT_PtPythonTopComponent=This is a PtPython window"
})
public final class PtPythonTopComponent extends TopComponent {

    private static final long serialVersionUID = 1L;
    static JediTermWidget jediTermWidget;
    static TtyConnector ttyConnector;

    public PtPythonTopComponent() {
        initComponents();
        setName(Bundle.CTL_PtPythonTopComponent());
        setToolTipText(Bundle.HINT_PtPythonTopComponent());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        SwingUtilities.invokeLater(() -> {
            ProgressHandle pg = ProgressHandle.createHandle(Bundle.CTL_Start());
            pg.switchToIndeterminate();
            pg.start();
            pg.progress(Bundle.CTL_Opening());
            setLayout(new GridLayout());
            jediTermWidget = new JediTermWidget(new DefaultSettingsProvider() {
                @Override
                public Font getTerminalFont() {
                    return new Font(PythonUtility.getTermFontFam(),
                            PythonUtility.geTermFontSt(),
                            PythonUtility.getTermFontSize());

                }
            });
            jediTermWidget.addListener((TerminalWidget tw) -> {
                EventQueue.invokeLater(() -> {
                    close();
                });
            });
            Project p = TopComponent.getRegistry().getActivated().getLookup().lookup(Project.class);
            if (p == null) {
                DataObject dob = TopComponent.getRegistry().getActivated().getLookup().lookup(DataObject.class);
                if (dob != null) {
                    FileObject fo = dob.getPrimaryFile();
                    p = FileOwnerQuery.getOwner(fo);
                }
            }

            ttyConnector = PythonTerminalConnector
                    .createTtyConnector("ptpython", (PythonProject) p);
            jediTermWidget.setTtyConnector(ttyConnector);
            jediTermWidget.start();
            removeAll();
            add(jediTermWidget);
            pg.finish();
        });
    }

    @Override
    public void componentClosed() {
        ttyConnector.close();
        jediTermWidget.close();
        removeAll();
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        //  store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        //  read your settings according to their version
    }
}
