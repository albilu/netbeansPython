package org.netbeans.modules.python.debugger;

import org.netbeans.api.debugger.DebuggerManagerAdapter;
import org.netbeans.api.debugger.LazyDebuggerManagerListener;
import org.netbeans.api.debugger.Session;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;

/**
 *
 * @author albilu
 */
@DebuggerServiceRegistration(types = LazyDebuggerManagerListener.class)
public class PythonDebuggerSessionListener extends DebuggerManagerAdapter {

    @Override
    public void sessionAdded(Session session) {
        super.sessionAdded(session);
    }

    @Override
    public void sessionRemoved(Session session) {
        super.sessionRemoved(session);
    }

}
