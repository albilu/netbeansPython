package org.netbeans.modules.python;

import java.io.IOException;
import java.nio.file.Files;
import kong.unirest.Unirest;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

@NbBundle.Messages("CTL_InstallError=Error while installing dependencies!")
public class Installer extends ModuleInstall {

    private static final long serialVersionUID = 1L;
    RequestProcessor requestProcessor = new RequestProcessor(this.getClass()
            .getName(), 1
    );

////TODO
//1. Debugger
//https://dzone.com/articles/how-reuse-netbeans-debugger
////LATER
//1. support pytest
//2. profiler(inspire from spyder) (check for libraries)
//3. support django and flask https://code.visualstudio.com/docs/python/tutorial-django
//4. Jupyter Notebook/Data science support/conda
    @Override
    public void restored() {
        Unirest.config().cacheResponses(true);
        if (NbPreferences.root().getBoolean("autoUpdate", true)) {
            requestProcessor.post(() -> {
                if (PythonUtility.installLsp(this.getClass().getClassLoader()) != 0) {
                    throw new PythonCustomException(Bundle.CTL_InstallError());
                }
            });
        }
    }

    @Override
    public void close() {
        if (super.closing()) {
            try {
                Files.deleteIfExists(PythonUtility.PYLSP_VENV_DIR.toPath().resolve("lsp_log_file"));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}
