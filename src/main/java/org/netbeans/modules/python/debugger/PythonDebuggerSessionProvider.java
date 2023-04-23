package org.netbeans.modules.python.debugger;

import static org.netbeans.modules.python.debugger.PythonDebugger.PYTHON_DEBUGGER_INFO;
import static org.netbeans.modules.python.debugger.PythonDebugger.PYTHON_SESSION;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.SessionProvider;

/**
 *
 * @author albilu
 */
@SessionProvider.Registration(path = PYTHON_DEBUGGER_INFO)
public class PythonDebuggerSessionProvider extends SessionProvider {

    private final ContextProvider contextProvider;

    public PythonDebuggerSessionProvider(ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
    }

    @Override
    public String getSessionName() {
        PythonDebuggerName lookupFirst = contextProvider
                .lookupFirst(null, PythonDebuggerName.class);
        return lookupFirst.toString();
    }

    @Override
    public String getLocationName() {
        return "localhost";
    }

    @Override
    public String getTypeID() {
        return PYTHON_SESSION;
    }

    @Override
    public Object[] getServices() {
        return new Object[]{};
    }

}
