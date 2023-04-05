package org.netbeans.modules.python.indexing;

import org.netbeans.modules.csl.spi.DefaultError;
import org.netbeans.modules.parsing.spi.indexing.ErrorsCache;

/**
 *
 * @author albilu
 */
public class PythonErrorConvertor implements ErrorsCache.Convertor<DefaultError> {

    @Override
    public ErrorsCache.ErrorKind getKind(DefaultError t) {
        return ErrorsCache.ErrorKind.ERROR;
    }

    @Override
    public int getLineNumber(DefaultError t) {
        return 1;
    }

    @Override
    public String getMessage(DefaultError t) {
        return "PythonError";
    }

}
