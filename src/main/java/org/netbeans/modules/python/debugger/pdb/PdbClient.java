package org.netbeans.modules.python.debugger.pdb;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.modules.python.debugger.PythonDebuggerCallStack;
import org.netbeans.modules.python.debugger.PythonDebuggerUtils;
import org.netbeans.modules.python.debugger.models.PythonDebuggerBreakpointsListener;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotatable;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.windows.IOColorLines;
import org.openide.windows.InputOutput;

/**
 *
 * @author albilu
 */
public class PdbClient {

    Process process;
    Pattern CALL_STACK = Pattern.compile("^(\\s+|>\\s)(.+\\.py)\\((\\d+)\\)(.*)\\(", Pattern.MULTILINE);
    Pattern CURRENT_PATTERN = Pattern.compile(".*>\\s+(.+\\.py)\\((\\d+)\\)");
    Pattern BREAKPOINT_PATTERN = Pattern.compile(".*(Breakpoint|Disabled breakpoint|Enabled breakpoint|Deleted breakpoint)\\s+(\\d+)\\s+at\\s+(.+\\.py):(\\d+)");

    boolean isStopped = false;
    InputOutput io;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    public PdbClient(Process process, InputOutput io) {
        this.process = process;
        this.io = io;
        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    }

    public Process getProcess() {
        return process;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    public void sendCommand(String cmd) throws IOException {
        isStopped = false;
        writer.write(cmd);
        writer.newLine();
        writer.flush();
    }

    public boolean sendCommandAndProcessResponse(String cmd) throws IOException {
        isStopped = false;
        writer.write(cmd);
        writer.newLine();
        writer.flush();
        try {
            return processResponse(getStreamResponse(false));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return false;
    }

    public Object[] sendBreakpointsCommand(String cmd) throws IOException {
        isStopped = false;
        writer.write(cmd);
        writer.newLine();
        writer.flush();
        try {
            return breakPointCommandResponse(getStreamResponse(false));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return new Object[]{false};

    }

    public List<PythonDebuggerCallStack> sendWhere(String cmd) throws IOException {
        isStopped = false;
        writer.write(cmd);
        writer.newLine();
        writer.flush();
        return whereResponse(getStreamResponse(false));

    }

    public String sendCommandAndGetResponse(String cmd) throws IOException {
        isStopped = false;
        writer.write(cmd);
        writer.newLine();
        writer.flush();
        return StringUtils.removeEnd(getStreamResponse(false), "(Pdb)").trim();
    }

    public String getStreamResponse(boolean first) throws IOException {
        //FIXME Something wrong with Python 2.7 reader.
        //Not supporting for now
        StringBuilder content = new StringBuilder();
        int value = reader.read();
        while (value != -1) {
            content.append((char) value);
            value = reader.read();
            if (StringUtils.endsWithAny(content.toString(), new String[]{"(Pdb) ", "(Pdb)"})) {
                break;
            }
            if (first && StringUtils.startsWith(content.toString(), "->")) {
                break;
            }
        }
        //System.out.println(content.toString());
        String toString = content.toString();
        boolean containsAnyIgnoreCase = StringUtils.containsAnyIgnoreCase(toString,
                new String[]{"***", "error", "Traceback"});

        IOColorLines.println(io, toString, containsAnyIgnoreCase ? Color.RED : Color.BLACK);
        return toString;
    }

    public Object[] breakPointCommandResponse(String response) {
        MatchResult breakMatcher = BREAKPOINT_PATTERN.matcher(response).results()
                .reduce((first, second) -> second).orElse(null);
        if (breakMatcher != null) {
            int id = NumberUtils.toInt(breakMatcher.group(2));
            return new Object[]{true, id};
        }
        return new Object[]{false};

    }

    public boolean processResponse(String response) {
        MatchResult matcher = CURRENT_PATTERN.matcher(response).results()
                .reduce((first, second) -> second).orElse(null);
        if (matcher != null) {
            String filePath = matcher.group(1);
            int lineNumber = NumberUtils.toInt(matcher.group(2));

            PythonDebuggerBreakpointsListener.firePropertiesChange();
            PythonDebuggerUtils.markCurrent(new Annotatable[]{getLine(filePath, lineNumber - 1)});
            isStopped = true;
            DebuggerEngine currentEngine = DebuggerManager.getDebuggerManager().getCurrentEngine();
            if (currentEngine != null) {
                PythonDebugger lookupFirst = currentEngine.lookupFirst(null, PythonDebugger.class);
                if (lookupFirst != null) {
                    lookupFirst.fireSessionState();
                    lookupFirst.fireCurrentThread();
                }
            }
            return true;

        }

        return false;
    }

    public List<PythonDebuggerCallStack> whereResponse(String response) {

        List<PythonDebuggerCallStack> stacks = new ArrayList();
        CALL_STACK.matcher(response).results()
                .forEach((t) -> {
                    String filePath = t.group(2);
                    int lineNumber = NumberUtils.toInt(t.group(3));
                    String method = t.group(4);
                    stacks.add(new PythonDebuggerCallStack(filePath, lineNumber, method));
                });

        return stacks;

    }

    private Annotatable getLine(String filePath, int lineNumber) {
        FileObject fobj = FileUtil.toFileObject(Paths.get(filePath).toFile());
        DataObject dobj = null;
        try {
            dobj = DataObject.find(fobj);
        } catch (DataObjectNotFoundException ex) {
        }
        if (dobj != null) {
            LineCookie lc = (LineCookie) dobj.getLookup().lookup(LineCookie.class);
            if (lc == null) {
                /* cannot do it */ return null;
            }
            Line l = lc.getLineSet().getOriginal(lineNumber);
            return l;
        }
        return null;
    }

    public boolean isFinished() {
        return !process.isAlive();
    }

    public boolean isSuspended() {
        return isStopped;
    }

}
