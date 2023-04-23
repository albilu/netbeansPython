package org.netbeans.modules.python.debugger;

import java.net.URI;
import java.nio.file.Paths;
import org.netbeans.spi.debugger.ui.DebuggingView;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonDebuggerCallStack implements DebuggingView.DVFrame {

    String filePath;
    int lineNumber;
    String method;
    private boolean current;

    public PythonDebuggerCallStack(String filePath, int lineNumber, String method) {
        this.filePath = filePath;
        this.lineNumber = lineNumber;
        this.method = method;
    }

    @Override
    public String getName() {
        return method;
    }

    @Override
    public DebuggingView.DVThread getThread() {
        return null;
    }

    @Override
    public void makeCurrent() {
        current = true;
    }

    public boolean isCurrent() {
        return current;
    }

    @Override
    public URI getSourceURI() {
        return Paths.get(filePath).toUri();
    }

    @Override
    public int getLine() {
        return lineNumber;
    }

    @Override
    public int getColumn() {
        return 0;
    }

    public Line location() {
        try {
            LineCookie lc = DataObject.find(FileUtil.toFileObject(Paths.get(getSourceURI())))
                    .getLookup().lookup(LineCookie.class);
            if (lc == null) {
                return null;
            }
            return lc.getLineSet().getOriginal(lineNumber - 1);
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

}
