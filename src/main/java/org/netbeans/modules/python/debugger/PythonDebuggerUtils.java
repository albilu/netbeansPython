package org.netbeans.modules.python.debugger;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.modules.python.debugger.breakpoints.PythonBreakpoint;
import org.netbeans.modules.python.debugger.pdb.PdbClient;
import org.openide.text.Annotatable;
import org.openide.text.Line;
import org.openide.text.Line.ShowOpenType;
import org.openide.text.Line.ShowVisibilityType;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonDebuggerUtils {

    private static Object currentLine;
    private static ArrayList<DebuggerAnnotation> markedStacks;

    public static synchronized void markCurrent(final Object line) {
        unmarkCurrent();

        Annotatable[] annotatables = (Annotatable[]) line;
        int i = 0, k = annotatables.length;

        // first line with icon in gutter
        DebuggerAnnotation[] annotations = new DebuggerAnnotation[k];
        if (annotatables[i] instanceof Line.Part) {
            annotations[i] = new DebuggerAnnotation(
                    DebuggerAnnotation.CURRENT_LINE_PART_ANNOTATION_TYPE,
                    annotatables[i]
            );
        } else {
            annotations[i] = new DebuggerAnnotation(
                    DebuggerAnnotation.CURRENT_LINE_ANNOTATION_TYPE,
                    annotatables[i]
            );
        }

        // other lines
        for (i = 1; i < k; i++) {
            if (annotatables[i] instanceof Line.Part) {
                annotations[i] = new DebuggerAnnotation(
                        DebuggerAnnotation.CURRENT_LINE_PART_ANNOTATION_TYPE2,
                        annotatables[i]
                );
            } else {
                annotations[i] = new DebuggerAnnotation(
                        DebuggerAnnotation.CURRENT_LINE_ANNOTATION_TYPE2,
                        annotatables[i]
                );
            }
        }
        currentLine = annotations;

        PythonDebugger debugger = getDebugger();
        debugger.currentLine = line;

        showLine(line);
    }

    public static synchronized void unmarkCurrent() {
        if (currentLine != null) {
            //((DebuggerAnnotation) currentLine).detach ();
            int i, k = ((DebuggerAnnotation[]) currentLine).length;
            for (i = 0; i < k; i++) {
                ((DebuggerAnnotation[]) currentLine)[i].detach();
            }
            currentLine = null;
        }
    }

    public static synchronized void markStacks(List callStacks) {
        unmarkStacks();
        if (callStacks.isEmpty()) {
            return;
        }
        callStacks.remove(0);//remove the current
        ArrayList<DebuggerAnnotation> stackAnnotations = new ArrayList<>();
        for (Object callStack : callStacks) {
            PythonDebuggerCallStack stack = (PythonDebuggerCallStack) callStack;

            Line location = stack.location();
            if (location == null) {
                return;
            }
            Annotatable[] annotatables = (Annotatable[]) new Annotatable[]{location};
            stackAnnotations.add(new DebuggerAnnotation(
                    DebuggerAnnotation.CALL_STACK_FRAME_ANNOTATION_TYPE,
                    annotatables[0]));

        }
        markedStacks = stackAnnotations;
    }

    public static synchronized void unmarkStacks() {
        if (markedStacks != null) {
            //((DebuggerAnnotation) markedStacks).detach ();
            for (DebuggerAnnotation markedStack : markedStacks) {
                markedStack.detach();
            }
            markedStacks = null;
        }
    }

    public static void showLine(final Object line) {
        final Annotatable[] a = (Annotatable[]) line;
        SwingUtilities.invokeLater(() -> {
            if (a[0] instanceof Line) {
                ((Line) a[0]).show(ShowOpenType.OPEN, ShowVisibilityType.FOCUS);
            } else if (a[0] instanceof Line.Part) {
                ((Line.Part) a[0]).getLine().show(ShowOpenType.OPEN, ShowVisibilityType.FOCUS);
            } else {
                throw new InternalError(a[0].toString());
            }
        });
    }

    public static boolean contains(Object currentLine, Line line) {
        if (currentLine == null) {
            return false;
        }
        final Annotatable[] a = (Annotatable[]) currentLine;
        int i, k = a.length;
        for (i = 0; i < k; i++) {
            if (a[i].equals(line)) {
                return true;
            }
            if (a[i] instanceof Line.Part
                    && ((Line.Part) a[i]).getLine().equals(line)) {
                return true;
            }
        }
        return false;
    }

    public static boolean managePdbBreakPoints(PythonBreakpoint pb, String action) {
        try {
            Session currentSession = DebuggerManager.getDebuggerManager().getCurrentSession();
            if (currentSession == null) {
                return true;
            }

            if (!currentSession.getCurrentLanguage().equals(PythonDebugger.PYTHON_DEBUGGER_LANGUAGE)) {
                return true;
            }
            if (pb.getValidity().equals(Breakpoint.VALIDITY.INVALID)) {
                return true;
            }

            DebuggerEngine currentEngine = currentSession.getCurrentEngine();
            PythonDebugger debugger = currentEngine.lookupFirst(null, PythonDebugger.class);
            PdbClient pdbClient = debugger.getPdbClient();
            String condition = pb.getCondition();

            switch (action) {
                case "enable":
                    Object[] responseE = pdbClient.sendBreakpointsCommand(String
                            .format("%s %s", "enable", pb.getID()));
                    boolean validE = (boolean) responseE[0];
                    if (validE) {
                        pb.setID((int) responseE[1]);
                    }
                    return validE;
                //dapClient.addBreakpoints(pb);
                case "disable":
                    Object[] responseD = pdbClient.sendBreakpointsCommand(String
                            .format("%s %s", "disable", pb.getID()));
                    boolean validD = (boolean) responseD[0];
                    if (validD) {
                        pb.setID((int) responseD[1]);
                    }
                    return validD;
                //dapClient.removeBreakpoints(pb);
                case "add":
                    String command = String.format("%s %s:%s", "break", pb
                            .getFileObject().getPath(), pb.getLineNumber());
                    if (condition != null && !condition.trim().isEmpty()) {
                        command = String.format("%s , %s", command, condition);
                    }
                    Object[] responseA = pdbClient.sendBreakpointsCommand(command);
                    boolean validA = (boolean) responseA[0];
                    if (validA) {
                        pb.setID((int) responseA[1]);
                    }
                    return validA;
                //dapClient.addBreakpoints(pb);
                case "remove":
                    Object[] responseR = pdbClient.sendBreakpointsCommand(String
                            .format("%s %s:%s", "clear", pb.getFileObject().getPath(),
                                    pb.getLineNumber()));
                    if (Utilities.isWindows()) {
                        return true;//for no reason on windows there is a blank response for this command
                    }
                    boolean validR = (boolean) responseR[0];
                    if (validR) {
                        pb.setID((int) responseR[1]);
                    }
                    return validR;
                //dapClient.removeBreakpoints(pb);
                //case "update":
                //dapClient.updateBreakpoints(pb);
                default:
                    return false;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private static PythonDebugger getDebugger() {
        DebuggerEngine engine = DebuggerManager.getDebuggerManager().
                getCurrentEngine();
        if (engine == null) {
            return null;
        }
        return engine.lookupFirst(null, PythonDebugger.class);
    }

    public static boolean isCurrent() {
        return currentLine != null;
    }

    public static String toHTML(String text, boolean bold, boolean italics, Color color) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        if (bold) {
            sb.append("<b>");
        }
        if (italics) {
            sb.append("<i>");
        }
        if (color == null) {
            color = UIManager.getColor("Table.foreground");
            if (color == null) {
                color = new JTable().getForeground();
            }
        }
        sb.append("<font color=\"#");
        String hexColor = Integer.toHexString((color.getRGB() & 0xffffff));
        for (int i = hexColor.length(); i < 6; i++) {
            sb.append("0"); // Prepend zeros to length of 6
        }
        sb.append(hexColor);
        sb.append("\">");
        text = text.replace("&", "&amp;");
        text = text.replace("<", "&lt;");
        text = text.replace(">", "&gt;");
        sb.append(text);
        sb.append("</font>");
        if (italics) {
            sb.append("</i>");
        }
        if (bold) {
            sb.append("</b>");
        }
        sb.append("</html>");
        return sb.toString();
    }

}
