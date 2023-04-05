package org.netbeans.modules.python;

/**
 *
 */
import java.io.IOException;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

@MimeRegistration(mimeType = PythonUtility.PYTHON_MIME_TYPE,
        service = LanguageServerProvider.class)
public class PythonLspClient implements LanguageServerProvider {

    @Override
    public LanguageServerDescription startServer(Lookup lkp) {
        try {
            Process p = new ProcessBuilder(
                    PythonUtility.getLspPythonExe(),
                    "-m",
                    "pylsp",
                    "--log-file",
                    PythonUtility.PYLSP_VENV_DIR.toPath().resolve("lsp_log_file").toString()
            //,
            //"-v"
            )
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            return LanguageServerDescription.create(p.getInputStream(),
                    p.getOutputStream(), p
            );
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

}
