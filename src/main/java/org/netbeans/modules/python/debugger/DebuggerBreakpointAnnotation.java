package org.netbeans.modules.python.debugger;

import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.Breakpoint.HIT_COUNT_FILTERING_STYLE;
import org.netbeans.modules.python.debugger.breakpoints.PythonBreakpoint;
import org.netbeans.spi.debugger.ui.BreakpointAnnotation;
import org.openide.text.Annotatable;
import org.openide.text.Line;
import org.openide.util.NbBundle;

/**
 * Debugger Annotation class.
 *
 * @author albilu
 */
public class DebuggerBreakpointAnnotation extends BreakpointAnnotation {

    public static final String BREAKPOINT_ANNOTATION_TYPE = "Breakpoint";
    public static final String DISABLED_BREAKPOINT_ANNOTATION_TYPE = "DisabledBreakpoint";
    public static final String CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE = "CondBreakpoint";
    public static final String DISABLED_CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE = "DisabledCondBreakpoint";

    private final String type;
    private final PythonBreakpoint breakpoint;

    private DebuggerBreakpointAnnotation(String type, Annotatable annotatable, PythonBreakpoint b) {
        this.type = type;
        this.breakpoint = b;
        attach(annotatable);
    }

    @CheckForNull
    public static DebuggerBreakpointAnnotation create(String type, PythonBreakpoint b) {
        Line line = b.getLine();
        if (line == null) {
            return null;
        }
        return new DebuggerBreakpointAnnotation(type, line, b);
    }

    @Override
    public String getAnnotationType() {
        return type;
    }

    @Override
    @NbBundle.Messages({"TTP_Breakpoint_Hits=Hits when:",
        "# {0} - hit count",
        "TTP_Breakpoint_HitsEqual=Hit count \\= {0}",
        "# {0} - hit count",
        "TTP_Breakpoint_HitsGreaterThan=Hit count > {0}",
        "# {0} - hit count",
        "TTP_Breakpoint_HitsMultipleOf=Hit count is multiple of {0}"})
    public String getShortDescription() {
        List<String> list = new LinkedList<>();
        //add condition if available
        String condition = breakpoint.getCondition();
        if (condition != null) {
            list.add(condition);
        }

        // add hit count if available
        HIT_COUNT_FILTERING_STYLE hitCountFilteringStyle = breakpoint.getHitCountFilteringStyle();
        if (hitCountFilteringStyle != null) {
            int hcf = breakpoint.getHitCountFilter();
            String tooltip;
            switch (hitCountFilteringStyle) {
                case EQUAL:
                    tooltip = Bundle.TTP_Breakpoint_HitsEqual(hcf);
                    break;
                case GREATER:
                    tooltip = Bundle.TTP_Breakpoint_HitsGreaterThan(hcf);
                    break;
                case MULTIPLE:
                    tooltip = Bundle.TTP_Breakpoint_HitsMultipleOf(hcf);
                    break;
                default:
                    throw new IllegalStateException("Unknown HitCountFilteringStyle: "
                            + hitCountFilteringStyle);
            }
            list.add(tooltip);
        }

        String typeDesc = getBPTypeDescription();
        if (list.isEmpty()) {
            return typeDesc;
        }
        StringBuilder result = new StringBuilder(typeDesc);
        //append more information
        result.append("\n");
        result.append(Bundle.TTP_Breakpoint_Hits());
        for (String text : list) {
            result.append("\n");
            result.append(text);
        }
        return result.toString();
    }

    @NbBundle.Messages({"TTP_Breakpoint=Breakpoint",
        "TTP_BreakpointDisabled=Disabled Breakpoint",
        "TTP_BreakpointConditional=Conditional Breakpoint",
        "TTP_BreakpointDisabledConditional=Disabled Conditional Breakpoint",
        "TTP_BreakpointBroken=Broken breakpoint - It is not possible to stop on this line.",
        "# {0} - Reason for being invalid",
        "TTP_BreakpointBrokenInvalid=Broken breakpoint: {0}",
        "TTP_BreakpointStroke=Deactivated breakpoint"})
    private String getBPTypeDescription() {
        if (type.endsWith("_broken")) {
            if (breakpoint.getValidity() == Breakpoint.VALIDITY.INVALID) {
                String msg = breakpoint.getValidityMessage();
                return Bundle.TTP_BreakpointBrokenInvalid(msg);
            }
            return Bundle.TTP_BreakpointBroken();
        }
        if (type.endsWith("_stroke")) {
            return Bundle.TTP_BreakpointStroke();
        }
        switch (type) {
            case BREAKPOINT_ANNOTATION_TYPE:
                return Bundle.TTP_Breakpoint();
            case DISABLED_BREAKPOINT_ANNOTATION_TYPE:
                return Bundle.TTP_BreakpointDisabled();
            case CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE:
                return Bundle.TTP_BreakpointConditional();
            case DISABLED_CONDITIONAL_BREAKPOINT_ANNOTATION_TYPE:
                return Bundle.TTP_BreakpointDisabledConditional();
            default:
                throw new IllegalStateException(type);
        }
    }

    @Override
    public Breakpoint getBreakpoint() {
        return breakpoint;
    }

}
