package org.netbeans.modules.python.debugger;

import org.openide.text.Annotatable;
import org.openide.text.Annotation;
import org.openide.util.NbBundle;

/**
 * Debugger Annotation class.
 *
 * @author albilu
 */
public class DebuggerAnnotation extends Annotation {

    public static final String CURRENT_LINE_ANNOTATION_TYPE = "CurrentPC";
    public static final String CURRENT_LINE_ANNOTATION_TYPE2 = "CurrentPC2";
    public static final String CURRENT_LINE_PART_ANNOTATION_TYPE = "CurrentPCLinePart";
    public static final String CURRENT_LINE_PART_ANNOTATION_TYPE2 = "CurrentPC2LinePart";
    public static final String CALL_STACK_FRAME_ANNOTATION_TYPE = "CallSite";

    private final String type;

    public DebuggerAnnotation(String type, Annotatable annotatable) {
        this.type = type;
        attach(annotatable);
    }

    @Override
    public String getAnnotationType() {
        return type;
    }

    @Override
    @NbBundle.Messages({"TTP_CurrentPC=Current Program Counter",
        "TTP_CurrentPC2=Current Target",
        "TTP_Callsite=Call Stack Line"})
    public String getShortDescription() {
        switch (type) {
            case CURRENT_LINE_ANNOTATION_TYPE:
            case CURRENT_LINE_PART_ANNOTATION_TYPE:
                return Bundle.TTP_CurrentPC();
            case CURRENT_LINE_ANNOTATION_TYPE2:
                return Bundle.TTP_CurrentPC2();
            case CALL_STACK_FRAME_ANNOTATION_TYPE:
                return Bundle.TTP_Callsite();
            default:
                throw new IllegalStateException(type);
        }
    }
}
