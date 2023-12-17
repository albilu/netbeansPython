package org.netbeans.modules.python.log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.python.PythonUtility;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "org.netbeans.modules.python.log.PythonLspLog"
)
@ActionRegistration(
        displayName = "#CTL_PythonLspLog"
)
@ActionReference(path = "Menu/View", position = 550)
@Messages("CTL_PythonLspLog=Python Lsp Log")
public final class PythonLspLog implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

        File f = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("lsp_log_file").toFile();

        if (!f.exists()) {
            return;
        }

        LogViewerSupport p = new LogViewerSupport(f, Bundle.CTL_PythonLspLog());
        try {
            p.showLogViewer();
        } catch (IOException ex) {
            Logger.getLogger(PythonLspLog.class.getName()).log(Level.INFO, "Showing Python Lsp log action failed", ex);
        }
    }
}
