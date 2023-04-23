package org.netbeans.modules.python.debugger;

import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.spi.debugger.DebuggerEngineProvider;

/**
 *
 * @author albilu
 */
public class PythonDebuggerProvider extends DebuggerEngineProvider {

    private static final String PYTHON_DEBUGGER_ENGINE = "PYthonDebuggerEngine";
    private DebuggerEngine.Destructor destructor;

    @Override
    public String[] getLanguages() {
        return new String[]{PythonDebugger.PYTHON_DEBUGGER_LANGUAGE};
    }

    @Override
    public String getEngineTypeID() {
        return PYTHON_DEBUGGER_ENGINE;
    }

    @Override
    public Object[] getServices() {
        return new Object[]{};
    }

    @Override
    public void setDestructor(DebuggerEngine.Destructor d) {
        this.destructor = d;
    }

    public DebuggerEngine.Destructor getDestructor() {
        return destructor;
    }

}
