package org.netbeans.modules.python.debugger.models;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.event.ChangeEvent;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
import org.netbeans.modules.python.debugger.breakpoints.PythonBreakpoint;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.NodeModel;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@DebuggerServiceRegistration(path = "BreakpointsView", types = {NodeModel.class})
public class PythonDebuggerBreakpointModel implements NodeModel {

    public static final String LINE_BREAKPOINT
            = "org/netbeans/modules/debugger/resources/breakpointsView/Breakpoint";
    public static final String LINE_BREAKPOINT_PC
            = "org/netbeans/modules/debugger/resources/breakpointsView/BreakpointHit";
    public static final String DISABLED_LINE_BREAKPOINT
            = "org/netbeans/modules/debugger/resources/breakpointsView/DisabledBreakpoint";

    private final List<ModelListener> listeners = new CopyOnWriteArrayList<>();

    RequestProcessor RP = new RequestProcessor(this.getClass().getName(), 5);

    public PythonDebuggerBreakpointModel() {
        PythonDebuggerBreakpointsListener.addChangeListener((ChangeEvent ce) -> {
            RP.post(() -> {
                fireChanges();
            });
        });
    }

    // NodeModel implementation ................................................
    /**
     * Returns display name for given node.
     *
     * @throws ComputingException if the display name resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve display name for given node type
     * @return display name for given node
     */
    @Override
    public String getDisplayName(Object node) throws UnknownTypeException {
        if (node instanceof PythonBreakpoint) {
            PythonBreakpoint breakpoint = (PythonBreakpoint) node;
            String nameExt;
            FileObject fileObject = breakpoint.getFileObject();
            if (fileObject != null) {
                nameExt = fileObject.getNameExt();
            } else {
                File file = new File(breakpoint.getFilePath());
                nameExt = file.getName();
            }
            PythonDebugger debugger = getDebugger();
            String name = String.format("%s %s:%s", "Line", nameExt, breakpoint.getLineNumber());
            return debugger != null
                    && PythonDebuggerUtils.contains(
                            debugger.getCurrentLine(),
                            breakpoint.getLine()
                    ) ? PythonDebuggerUtils.toHTML(
                    name,
                    true,
                    false,
                    null
            ) : name;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns icon for given node.
     *
     * @throws ComputingException if the icon resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve icon for given node type
     * @return icon for given node
     */
    @Override
    public String getIconBase(Object node) throws UnknownTypeException {
        if (node instanceof PythonBreakpoint) {
            PythonBreakpoint breakpoint = (PythonBreakpoint) node;
            if (!((PythonBreakpoint) node).isEnabled()) {
                return DISABLED_LINE_BREAKPOINT;
            }
            PythonDebugger debugger = getDebugger();
            if (debugger != null
                    && PythonDebuggerUtils.contains(
                            debugger.getCurrentLine(),
                            breakpoint.getLine()
                    )) {
                return LINE_BREAKPOINT_PC;
            }
            return LINE_BREAKPOINT;
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Returns tooltip for given node.
     *
     * @throws ComputingException if the tooltip resolving process is time
     * consuming, and the value will be updated later
     * @throws UnknownTypeException if this NodeModel implementation is not able
     * to resolve tooltip for given node type
     * @return tooltip for given node
     */
    @Override
    public String getShortDescription(Object node)
            throws UnknownTypeException {
        if (node instanceof PythonBreakpoint) {
            PythonBreakpoint breakpoint = (PythonBreakpoint) node;
            return String.format("%s %s:%s", "Line", breakpoint.getFilePath(),
                    breakpoint.getLineNumber());
        }
        throw new UnknownTypeException(node);
    }

    /**
     * Registers given listener.
     *
     * @param l the listener to add
     */
    @Override
    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    /**
     * Unregisters given listener.
     *
     * @param l the listener to remove
     */
    @Override
    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }

    public void fireChanges() {
        ModelEvent event = new ModelEvent.TreeChanged(this);
        for (ModelListener l : listeners) {
            l.modelChanged(event);
        }
    }

    private static PythonDebugger getDebugger() {
        DebuggerEngine engine = DebuggerManager.getDebuggerManager().
                getCurrentEngine();
        if (engine == null) {
            return null;
        }
        return engine.lookupFirst(null, PythonDebugger.class);
    }

}
