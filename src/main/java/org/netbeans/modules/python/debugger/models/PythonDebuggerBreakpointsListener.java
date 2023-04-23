package org.netbeans.modules.python.debugger.models;

import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

/**
 *
 * @author albilu
 */
public class PythonDebuggerBreakpointsListener {

    private static final ChangeSupport cs = new ChangeSupport(PythonDebuggerBreakpointsListener.class);

    public static void addChangeListener(ChangeListener listener) {
        cs.addChangeListener(listener);
    }

    public static void removeChangeListener(ChangeListener listener) {
        cs.removeChangeListener(listener);
    }

    public static void firePropertiesChange() {
        cs.fireChange();
    }
}
