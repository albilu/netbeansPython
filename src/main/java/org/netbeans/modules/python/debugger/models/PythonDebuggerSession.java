package org.netbeans.modules.python.debugger.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.Session;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.Constants;
import org.netbeans.spi.viewmodel.ModelEvent;
import org.netbeans.spi.viewmodel.ModelListener;
import org.netbeans.spi.viewmodel.TableModel;
import org.netbeans.spi.viewmodel.TableModelFilter;
import org.netbeans.spi.viewmodel.UnknownTypeException;
import org.openide.util.NbBundle;

/**
 *
 * @author albilu
 */
@DebuggerServiceRegistration(path = "SessionsView", types = TableModelFilter.class)
public class PythonDebuggerSession implements TableModelFilter, Constants {

    private final List<Session> sessionListeners = new ArrayList<>();
    private final List<ModelListener> modelListeners = new CopyOnWriteArrayList<>();

    @Override
    public Object getValueAt(TableModel original, Object node, String columnID)
            throws UnknownTypeException {
        if (node instanceof Session && isPythonSession((Session) node)) {
            if (null == columnID) {
                throw new UnknownTypeException(node);
            } else {
                switch (columnID) {
                    case SESSION_STATE_COLUMN_ID:
                        return getSessionState((Session) node);
                    case SESSION_LANGUAGE_COLUMN_ID:
                        return node;
                    case SESSION_HOST_NAME_COLUMN_ID:
                        return ((Session) node).getLocationName();
                    default:
                        throw new UnknownTypeException(node);
                }
            }
        }
        return original.getValueAt(node, columnID);
    }

    @Override
    public boolean isReadOnly(TableModel original, Object node, String columnID)
            throws UnknownTypeException {
        if (node instanceof Session && isPythonSession((Session) node)) {
            if (null == columnID) {
                throw new UnknownTypeException(node);
            } else {
                switch (columnID) {
                    case SESSION_STATE_COLUMN_ID:
                        return true;
                    case SESSION_LANGUAGE_COLUMN_ID:
                        return false;
                    case SESSION_HOST_NAME_COLUMN_ID:
                        return true;
                    default:
                        throw new UnknownTypeException(node);
                }
            }
        }
        return original.isReadOnly(node, columnID);
    }

    @Override
    public void setValueAt(TableModel original, Object node, String columnID, Object value)
            throws UnknownTypeException {
        original.setValueAt(node, columnID, value);
    }

    private static boolean isPythonSession(Session s) {
        DebuggerEngine e = s.getCurrentEngine();
        if (e == null) {
            return false;
        }

        PythonDebugger d = e.lookupFirst(null, PythonDebugger.class);
        return d != null;
    }

    @NbBundle.Messages({"MSG_Session_State_Starting=Starting",
        "MSG_Session_State_Finished=Finished",
        "MSG_Session_State_Running=Running",
        "MSG_Session_State_Stopped=Stopped"})
    private String getSessionState(Session s) {
        DebuggerEngine e = s.getCurrentEngine();
        if (e == null) {
            return Bundle.MSG_Session_State_Starting();
        }
        PythonDebugger d = e.lookupFirst(null, PythonDebugger.class);
        if (d.isFinished()) {
            return Bundle.MSG_Session_State_Finished();
        }
        synchronized (sessionListeners) {
            if (!sessionListeners.contains(s)) {
                PythonDebugger.StateListener asl = new SessionStateListener(s, d);
                d.addStateListener(asl);
                sessionListeners.add(s);
            }
        }
        if (d.isSuspended()) {
            return Bundle.MSG_Session_State_Stopped();
        } else {
            return Bundle.MSG_Session_State_Running();
        }
    }

    private void fireModelChanged(Object node) {
        ModelEvent me = new ModelEvent.TableValueChanged(this, node, SESSION_STATE_COLUMN_ID);
        for (ModelListener ml : modelListeners) {
            ml.modelChanged(me);
        }
    }

    @Override
    public void addModelListener(ModelListener l) {
        modelListeners.add(l);
    }

    @Override
    public void removeModelListener(ModelListener l) {
        modelListeners.remove(l);
    }

    private class SessionStateListener implements PythonDebugger.StateListener {

        private final Session s;
        private final PythonDebugger d;

        SessionStateListener(Session s, PythonDebugger d) {
            this.s = s;
            this.d = d;
        }

        @Override
        public void suspended(boolean suspended) {
            fireModelChanged(s);
        }

        @Override
        public void finished() {
            fireModelChanged(s);
            d.removeStateListener(this);
            synchronized (sessionListeners) {
                sessionListeners.remove(s);
            }
        }

        @Override
        public void currentThread() {
        }

        @Override
        public void currentFrame() {
        }

    }
}
