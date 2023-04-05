package org.netbeans.modules.python.testrunner;

import java.util.Collection;
import javax.swing.event.ChangeListener;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.modules.python.PythonUtility;
import org.openide.util.RequestProcessor;

/**
 *
 * @author albilu
 */
@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = ParserFactory.class)
public class PythonTestParser extends ParserFactory {

    @Override
    public Parser createParser(Collection<Snapshot> snapshots) {
        return new PythonParser();
    }

    private static class PythonParser extends Parser {

        static RequestProcessor RP = new RequestProcessor(PythonParser.class);

        @Override
        public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
            RP.post(() -> PythonTestMethodController.setMethods(snapshot));
        }

        @Override
        public Result getResult(Task task) throws ParseException {
            return null;
        }

        @Override
        public void addChangeListener(ChangeListener changeListener) {
        }

        @Override
        public void removeChangeListener(ChangeListener changeListener) {
        }

    }

}
