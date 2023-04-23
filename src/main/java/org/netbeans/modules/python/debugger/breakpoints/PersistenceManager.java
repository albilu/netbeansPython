package org.netbeans.modules.python.debugger.breakpoints;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.LazyDebuggerManagerListener;
import org.netbeans.api.debugger.Properties;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.Watch;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;

/**
 * Listens on DebuggerManager and: - loads all breakpoints & watches on startup
 * - listens on all changes of breakpoints and watches (like breakoint / watch
 * added / removed, or some property change) and saves a new values
 *
 * @author albilu
 */
@DebuggerServiceRegistration(types = {LazyDebuggerManagerListener.class})
public class PersistenceManager implements LazyDebuggerManagerListener {

    private boolean areBreakpointsPersisted() {
        Properties p = Properties.getDefault().getProperties("debugger");
        p = p.getProperties("persistence");
        return p.getBoolean("breakpoints", true);
    }

    @Override
    public Breakpoint[] initBreakpoints() {
        if (!areBreakpointsPersisted()) {
            return new Breakpoint[]{};
        }
        Properties p = Properties.getDefault().getProperties("debugger").
                getProperties(DebuggerManager.PROP_BREAKPOINTS);
        Breakpoint[] breakpoints = (Breakpoint[]) p.getArray(
                "python",
                new Breakpoint[0]
        );
        for (int i = 0; i < breakpoints.length; i++) {
            if (breakpoints[i] == null) {
                Breakpoint[] b2 = new Breakpoint[breakpoints.length - 1];
                System.arraycopy(breakpoints, 0, b2, 0, i);
                if (i < breakpoints.length - 1) {
                    System.arraycopy(breakpoints, i + 1, b2, i, breakpoints.length - i - 1);
                }
                breakpoints = b2;
                i--;
                continue;
            }
            breakpoints[i].addPropertyChangeListener(this);
        }
        return breakpoints;
    }

    @Override
    public void initWatches() {
    }

    @Override
    public String[] getProperties() {
        return new String[]{
            DebuggerManager.PROP_BREAKPOINTS_INIT,
            DebuggerManager.PROP_BREAKPOINTS,};
    }

    @Override
    public void breakpointAdded(Breakpoint breakpoint) {
        if (!areBreakpointsPersisted()) {
            return;
        }
        if (breakpoint instanceof PythonBreakpoint) {
            Properties p = Properties.getDefault().getProperties("debugger").
                    getProperties(DebuggerManager.PROP_BREAKPOINTS);
            p.setArray(
                    "python",
                    getBreakpoints()
            );
            breakpoint.addPropertyChangeListener(this);
        }
    }

    @Override
    public void breakpointRemoved(Breakpoint breakpoint) {
        if (!areBreakpointsPersisted()) {
            return;
        }
        if (breakpoint instanceof PythonBreakpoint) {
            Properties p = Properties.getDefault().getProperties("debugger").
                    getProperties(DebuggerManager.PROP_BREAKPOINTS);
            p.setArray(
                    "python",
                    getBreakpoints()
            );
            breakpoint.removePropertyChangeListener(this);
        }
    }

    @Override
    public void watchAdded(Watch watch) {
    }

    @Override
    public void watchRemoved(Watch watch) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof Breakpoint) {
            Properties.getDefault().getProperties("debugger").
                    getProperties(DebuggerManager.PROP_BREAKPOINTS).setArray(
                    "python",
                    getBreakpoints()
            );
        }
    }

    @Override
    public void sessionAdded(Session session) {
    }

    @Override
    public void sessionRemoved(Session session) {
    }

    @Override
    public void engineAdded(DebuggerEngine engine) {
    }

    @Override
    public void engineRemoved(DebuggerEngine engine) {
    }

    private static Breakpoint[] getBreakpoints() {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager().
                getBreakpoints();
        List<Breakpoint> bb = new ArrayList<>();
        for (Breakpoint b : bs) {
            if (b instanceof PythonBreakpoint) {
                // Don't store hidden breakpoints
                if (!((PythonBreakpoint) b).isHidden()) {
                    bb.add(b);
                }
            }
        }
        bs = new Breakpoint[bb.size()];
        return bb.toArray(bs);
    }

}
