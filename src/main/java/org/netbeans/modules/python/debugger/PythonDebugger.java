package org.netbeans.modules.python.debugger;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerInfo;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.actions.PythonRun;
import org.netbeans.modules.python.debugger.breakpoints.PythonBreakpoint;
import org.netbeans.modules.python.debugger.pdb.PdbClient;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerEngineProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.IOColorLines;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 * The Python Debugger is mostly inspire from the CPPLiteDebugger & the
 * JavascriptV8 debugger
 *
 * @author albilu
 */
public class PythonDebugger {

    public static final String PYTHON_DEBUGGER_INFO = "PYthonDebuggerInfo";
    public static final String PYTHON_SESSION = "PYthonSession";
    public static final String PYTHON_DEBUGGER_LANGUAGE = "Python";

    private final PythonDebuggerProvider engineProvider;
    static PdbClient pdbClient;
    //static DAPClient dapClient;
    static ProgressHandle createHandle;
    static InputOutput io;

    private final List<StateListener> stateListeners = new CopyOnWriteArrayList<>();
    public volatile Object currentLine;

    public PythonDebugger(ContextProvider contextProvider) {
        // init engineProvider
        engineProvider = (PythonDebuggerProvider) contextProvider
                .lookupFirst(null, DebuggerEngineProvider.class);
    }

    @NbBundle.Messages({"CTL_MultiSession=There is already a session running. Multiple debug sessions"
        + " not supported at the moment",
        "CTL_SessionName=PDB Session"
    })
    public static void startDebugger(Project owner, DataObject dob, boolean singleFile) {
        try {
            List<String> runArgs = PythonRun.getRunArgs(owner, dob, true);

            if (runArgs.isEmpty()) {
                return;
            }
            if (PythonUtility.getVersion(runArgs.get(0)).contains("Python 2")) {
                DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message("Python 2 Debugging not supported at the moment",
                                NotifyDescriptor.INFORMATION_MESSAGE));
                return;
            }

            String sessionName = owner != null && !singleFile ? ProjectUtils
                    .getInformation(owner).getDisplayName()
                    : dob.getPrimaryFile().getNameExt();

            DebuggerManager manager = DebuggerManager.getDebuggerManager();

            boolean isRunning = false;
            Session[] sessions = manager.getSessions();
            for (Session session : sessions) {
                if (session.getCurrentLanguage().equals(PYTHON_DEBUGGER_LANGUAGE)) {
                    //already running
                    isRunning = true;
                }

            }
            //TODO Support multi session
            if (isRunning) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(
                        Bundle.CTL_MultiSession(), NotifyDescriptor.INFORMATION_MESSAGE));
                return;
            }

            DebuggerInfo info = DebuggerInfo.create(PYTHON_DEBUGGER_INFO,
                    new Object[]{new PythonDebuggerName(sessionName)});
            manager.startDebugging(info);

            runArgs.addAll(1, Arrays.asList("-m", "pdb"));

            //String[] command = {"python", "-m", "debugpy",
            //"--wait-for-client", "--listen", "localhost:5684", "/home/*/NetBeansProjects/PythonProject2/main.py"};
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(runArgs)
                    .directory(owner != null ? FileUtil.toFile(owner.getProjectDirectory())
                            : FileUtil.toFile(dob.getPrimaryFile().getParent()));
            PythonUtility.manageRunEnvs(pb);

            Process process = pb.redirectErrorStream(true).start();
            createHandle = ProgressHandle.createHandle(String.format("%s (%s)",
                    Bundle.CTL_SessionName(), sessionName));
            createHandle.start();
            createHandle.suspend("Running");

            io = IOProvider.getDefault().getIO(String.format("%s (%s)",
                    Bundle.CTL_SessionName(), sessionName), false);
            io.setInputVisible(true);
            io.select();

            pdbClient = new PdbClient(process, /*loggingPtyProcessTtyConnector,*/ io);

            IOColorLines.println(io, "[LOG]PythonDebugger: Starting new Debugging Session using PDB...", Color.BLUE);
            IOColorLines.println(io, "[LOG]This is a Lite wrapper around the built in Python debugger", Color.BLUE);
            IOColorLines.println(io, "[LOG]Please do not file bugs about this debugger, which is scheduled to be replaced.", Color.BLUE);

            //dapClient = new DAPClient("localhost", 5684);
            //dapClient.connect();
            // process first pdb output after start
            pdbClient.processResponse(pdbClient.getStreamResponse(true));

            // Populate breakpoints in Pdb
            Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
            for (Breakpoint breakpoint : breakpoints) {
                if (breakpoint instanceof PythonBreakpoint) {
                    boolean managePdbBreakPoints = PythonDebuggerUtils.managePdbBreakPoints((PythonBreakpoint) breakpoint, "add");
                    if (!managePdbBreakPoints) {
                        ((PythonBreakpoint) breakpoint)
                                .setBreakPointValidity(Breakpoint.VALIDITY.INVALID, "Invalid");
                    }
                }

            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    void kill() {
        try {
            // kill thread and process
            pdbClient.sendCommand("quit");
            pdbClient.getProcess().destroyForcibly();
            pdbClient.getReader().close();
            pdbClient.getWriter().close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        engineProvider.getDestructor().killEngine();
        io.closeInputOutput();
        createHandle.finish();
    }

    public PdbClient getPdbClient() {
        return pdbClient;
    }
    //void kill() {
    //// kill thread and process
    //dapClient.close();
    //engineProvider.getDestructor().killEngine();
    //}
    //    //public DAPClient getPdbClient() {
    //return dapClient;
    //}

    public Object getCurrentLine() {
        return currentLine;
    }

    public boolean isFinished() {
        return pdbClient.isFinished();
    }

    public boolean isSuspended() {
        return pdbClient.isSuspended();
    }

    public List getCallStacks() {
        try {
            return pdbClient.sendWhere("where");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public interface StateListener extends EventListener {

        void currentThread();

        void currentFrame();

        void suspended(boolean suspended);

        void finished();

    }

    public void addStateListener(StateListener sl) {
        stateListeners.add(sl);
    }

    public void removeStateListener(StateListener sl) {
        stateListeners.remove(sl);
    }

    public void fireSessionState() {
        for (StateListener sl : stateListeners) {
            sl.suspended(true);
        }
    }

    public void fireCurrentThread() {
        for (StateListener sl : stateListeners) {
            sl.currentThread();
        }
    }
}
