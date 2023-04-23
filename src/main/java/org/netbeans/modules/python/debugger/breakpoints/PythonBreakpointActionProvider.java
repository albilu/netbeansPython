package org.netbeans.modules.python.debugger.breakpoints;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
import org.netbeans.spi.debugger.ActionsProvider;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.filesystems.FileObject;
import org.openide.text.Line;
import org.openide.util.WeakListeners;

/**
 *
 * @author albilu
 */
@ActionsProvider.Registration(actions = {"toggleBreakpoint"},
        activateForMIMETypes = {PythonUtility.PYTHON_MIME_TYPE})
public class PythonBreakpointActionProvider extends ActionsProviderSupport
        implements PropertyChangeListener {

    private static final String[] PYTHON_MIME_TYPES = new String[]{PythonUtility.PYTHON_MIME_TYPE};
    private static final Set<String> PYTHON_MIME_TYPES_SET = new HashSet<>(Arrays.asList(PYTHON_MIME_TYPES));

    private static final Set ACTIONS = Collections.singleton(
            ActionsManager.ACTION_TOGGLE_BREAKPOINT
    );

    EditorContextDispatcher context = EditorContextDispatcher.getDefault();

    public PythonBreakpointActionProvider() {
        for (String mimeType : PYTHON_MIME_TYPES) {
            context.addPropertyChangeListener(mimeType,
                    WeakListeners.propertyChange(this, context));
        }
        setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, false);
    }

    /**
     * Called when the action is called (action button is pressed).
     *
     * @param action an action which has been called
     */
    @Override
    public void doAction(Object action) {
        Line line = getCurrentLine();
        if (line == null) {
            return;
        }

        Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
        FileObject fo = line.getLookup().lookup(FileObject.class);
        if (fo == null) {
            return;
        }
        int lineNumber = line.getLineNumber() + 1;
        int i, k = breakpoints.length;
        for (i = 0; i < k; i++) {
            if (breakpoints[i] instanceof PythonBreakpoint) {
                PythonBreakpoint pb = (PythonBreakpoint) breakpoints[i];
                if (fo.equals(pb.getFileObject()) && pb.getLineNumber() == lineNumber) {
                    boolean managePdbBreakPoints = PythonDebuggerUtils.managePdbBreakPoints(pb, "remove");
                    if (managePdbBreakPoints) {
                        DebuggerManager.getDebuggerManager().removeBreakpoint(pb);
                    }
                    break;
                }
            }
        }
        if (i == k) {
            PythonBreakpoint create = PythonBreakpoint.create(line);
            boolean managePdbBreakPoints = PythonDebuggerUtils.managePdbBreakPoints(create, "add");
            if (managePdbBreakPoints) {
                DebuggerManager.getDebuggerManager().addBreakpoint(
                        create
                );
            }

        }
    }

    /**
     * Returns set of actions supported by this ActionsProvider.
     *
     * @return set of actions supported by this ActionsProvider
     */
    @Override
    public Set getActions() {
        return ACTIONS;
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // We need to push the state there :-(( instead of wait for someone to be interested in...
        boolean enabled = getCurrentLine() != null;
        setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, enabled);
    }

}
