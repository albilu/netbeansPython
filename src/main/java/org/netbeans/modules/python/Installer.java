package org.netbeans.modules.python;

import java.io.IOException;
import java.nio.file.Files;
import kong.unirest.Unirest;
import org.openide.awt.CheckForUpdatesProvider;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
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
    //clean testrunner
    //clean ressources
    //clean projectproperties
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
        UpdateHandler.addUC();
        CheckForUpdatesProvider checkForUpdatesProvider = Lookup.getDefault().lookup(CheckForUpdatesProvider.class);
        checkForUpdatesProvider.notifyAvailableUpdates(true);
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
