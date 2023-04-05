package org.netbeans.modules.python;

import java.util.Collections;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.netbeans.api.extexecution.print.LineConvertors;

/**
 *
 * @author albilu
 */
public class PythonOutputLine implements ExecutionDescriptor.LineConvertorFactory {

    static LineConvertor LINE_CONVERTOR = (String line) -> {
        PythonUtility.noModuleInstalledHandler(line);
        return Collections.singletonList(ConvertedLine.forText(line, null));
    };

    @Override
    public LineConvertor newLineConvertor() {

        return LineConvertors.proxy(
                PythonUtility.HTTP_CONVERTOR,
                PythonUtility.FILE_CONVERTOR,
                LINE_CONVERTOR
        );
    }

}
