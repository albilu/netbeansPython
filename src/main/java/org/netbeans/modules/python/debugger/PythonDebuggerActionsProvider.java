package org.netbeans.modules.python.debugger;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.debugger.pdb.PdbClient;
import org.netbeans.spi.debugger.ActionsProvider;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@ActionsProvider.Registration(path = "PYthonSession", actions = {
    "start",
    "stepInto",
    "stepOver",
    "stepOut",
    "restart",
    "continue",
    "kill"
}, activateForMIMETypes = PythonUtility.PYTHON_MIME_TYPE)
public class PythonDebuggerActionsProvider extends ActionsProviderSupport {

    private static final Set<Object> ACTIONS = new HashSet();
    private static final Set<Object> ACTIONS_TO_DISABLE = new HashSet<>();

    private final PythonDebugger debugger;

    /**
     * The ReqeustProcessor used by action performers.
     */
    private static RequestProcessor actionsRequestProcessor;
    private static RequestProcessor killRequestProcessor;

    static PdbClient pdbClient;

    public PythonDebuggerActionsProvider(ContextProvider contextProvider) {
        //if we not enable the actions, our debugger will show them as greyed
        //out by default, in both the menus and the toolbar.
        debugger = contextProvider.lookupFirst(null, PythonDebugger.class);
        for (Iterator it = ACTIONS.iterator(); it.hasNext();) {
            setEnabled(it.next(), true);
        }
    }

    /**
     * Make an array of actions for convenience.
     */
    static {
        ACTIONS.add(ActionsManager.ACTION_KILL);
        ACTIONS.add(ActionsManager.ACTION_RESTART);
        ACTIONS.add(ActionsManager.ACTION_CONTINUE);
        ACTIONS.add(ActionsManager.ACTION_STEP_OVER);
        ACTIONS.add(ActionsManager.ACTION_STEP_INTO);
        ACTIONS.add(ActionsManager.ACTION_STEP_OUT);
        ACTIONS_TO_DISABLE.addAll(ACTIONS);
        // Ignore the KILL action
        ACTIONS_TO_DISABLE.remove(ActionsManager.ACTION_KILL);
    }

    /**
     * This method starts the debugger. Don't worry about creating a similar
     * stopDebugger method, as this is taken care of by our set of defined
     * actions. This method literally starts the debugger, by passing a
     * DebuggerInfo instance to NetBeans DebuggerManager class.
     */
    /**
     * This is where we implement (or delegate), the implementation of our
     * debugger. In other words, this is where we tell our debugger
     * implementation to step over, into, stop, or to take other custom
     * operations.
     *
     * @param action
     */
    @Override
    public void doAction(Object action) {
        pdbClient = debugger.getPdbClient();
        PythonDebuggerUtils.unmarkCurrent();
        PythonDebuggerUtils.unmarkStacks();
        try {
            if (action.equals(ActionsManager.ACTION_KILL)) {
                debugger.kill();
            } else if (action.equals(ActionsManager.ACTION_RESTART)) {
                pdbClient.sendCommandAndProcessResponse("restart");
            } else if (action.equals(ActionsManager.ACTION_CONTINUE)) {
                pdbClient.sendCommandAndProcessResponse("continue");
            } else if (action.equals(ActionsManager.ACTION_STEP_OVER)) {
                pdbClient.sendCommandAndProcessResponse("next");
            } else if (action.equals(ActionsManager.ACTION_STEP_INTO)) {
                pdbClient.sendCommandAndProcessResponse("step");
            } else if (action.equals(ActionsManager.ACTION_STEP_OUT)) {
                pdbClient.sendCommandAndProcessResponse("return");
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void postAction(final Object action, final Runnable actionPerformedNotifier) {
        if (action == ActionsManager.ACTION_KILL) {
            synchronized (PythonDebugger.class) {
                if (killRequestProcessor == null) {
                    killRequestProcessor = new RequestProcessor("PYthon debugger finish RP", 1);
                }
            }
            killRequestProcessor.post(() -> {
                try {
                    doAction(action);
                } finally {
                    actionPerformedNotifier.run();
                }
            });
            return;
        }
        setDebugActionsEnabled(false);
        synchronized (PythonDebugger.class) {
            if (actionsRequestProcessor == null) {
                actionsRequestProcessor = new RequestProcessor("PYthon debugger actions RP", 1);
            }
        }
        actionsRequestProcessor.post(() -> {
            try {
                doAction(action);
            } finally {
                actionPerformedNotifier.run();
                setDebugActionsEnabled(true);
            }
        });
    }

    private void setDebugActionsEnabled(boolean enabled) {
        for (Object action : ACTIONS_TO_DISABLE) {
            setEnabled(action, enabled);
        }
    }

    @Override
    public Set getActions() {
        return ACTIONS;
    }

}
