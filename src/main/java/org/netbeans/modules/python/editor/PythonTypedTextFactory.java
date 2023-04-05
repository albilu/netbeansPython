package org.netbeans.modules.python.editor;

import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.editor.typinghooks.TypedTextInterceptor;

/**
 *
 * @author albilu
 */
@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE, service = TypedTextInterceptor.Factory.class)
public class PythonTypedTextFactory implements TypedTextInterceptor.Factory {

    @Override
    public TypedTextInterceptor createTypedTextInterceptor(MimePath mimePath) {
        return new PythonTypedTextInterceptor();
    }

}
