package org.netbeans.modules.python.debugger.attach;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.swing.JComponent;
import org.netbeans.spi.debugger.ui.AttachType;
import org.netbeans.spi.debugger.ui.Controller;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
@NbBundle.Messages("CTL_PythonConnector_name=Python Debugger")
@AttachType.Registration(displayName = "#CTL_PythonConnector_name")
public class PythonDebuggerAttachType extends AttachType {

    private Reference<PythonDebuggerAttachCustomizer> customizerRef = new WeakReference<>(null);

    @Override
    public JComponent getCustomizer() {
        PythonDebuggerAttachCustomizer ac = new PythonDebuggerAttachCustomizer();
        customizerRef = new WeakReference<>(ac);
        return ac;
    }

    @Override
    public Controller getController() {
        PythonDebuggerAttachCustomizer panel = customizerRef.get();
        if (panel != null) {
            return panel.getController();
        } else {
            return null;
        }
    }

}
