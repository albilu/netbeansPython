package org.netbeans.modules.python;

/**
 *
 */
import java.io.IOException;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.Project;
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
                    "--verbose",
                    "--log-file",
                    PythonUtility.PYLSP_VENV_DIR.toPath().resolve("lsp_log_file").toString()
            //,
            //"-v"
            )
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();
            LanguageServerDescription description = LanguageServerDescription.create(p.getInputStream(),
                    p.getOutputStream(), p
            );
            try {
                PythonLspConfiguration.serverStarted(lkp.lookup(Project.class), description);
            } catch (RuntimeException | LinkageError ex) {
                // The server still works, only without the project's interpreter.
                Exceptions.printStackTrace(ex);
            }
            return description;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

}
