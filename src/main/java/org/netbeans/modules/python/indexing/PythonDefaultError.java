package org.netbeans.modules.python.indexing;

import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.DefaultError;
import org.openide.filesystems.FileObject;

/**
 *
 * @author albilu
 */
public class PythonDefaultError extends DefaultError implements org.netbeans.modules.csl.api.Error.Badging {

    public PythonDefaultError(String key, String displayName, String description,
            FileObject file, int start, int end, Severity severity) {
        super(key, displayName, description, file, start, end, severity);
    }

    @Override
    public boolean showExplorerBadge() {
        return true;
    }

    @Override
    public boolean isLineError() {
        return super.isLineError();
    }

    @Override
    public Object[] getParameters() {
        return super.getParameters();
    }

}
