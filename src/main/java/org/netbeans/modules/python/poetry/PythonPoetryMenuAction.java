package org.netbeans.modules.python.poetry;

import com.electronwill.nightconfig.core.file.FileConfig;
import java.awt.event.ActionEvent;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

@ActionID(category = "Project", id = "org.netbeans.modules.python.poetry.PythonPoetryMenuAction")
@ActionRegistration(displayName = "#CTL_PythonPoetryMenuAction", lazy = false, asynchronous = true) // NOI18N

@ActionReferences({
    @ActionReference(path = "Projects/org-netbeans-modules-python/Actions", position = 1205),
    @ActionReference(path = "Loaders/folder/any/Actions", position = 201)
})
@NbBundle.Messages({"CTL_PythonPoetryMenuAction=Poetry", "CTL_PythonPoetryRunScriptsMenu=Run Scripts"})
public class PythonPoetryMenuAction extends AbstractAction implements ContextAwareAction, Presenter.Popup {

    PythonProject p;
    Map poetryScripts;
    DataObject dataObject;

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public PythonPoetryMenuAction(PythonProject p, boolean b, Map poetryScripts) {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        setEnabled(b);
        this.p = p;
        this.poetryScripts = poetryScripts;
    }

    public PythonPoetryMenuAction() {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        setEnabled(false);
    }

    private PythonPoetryMenuAction(DataObject dataObject, boolean b) {
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        setEnabled(b);
        this.dataObject = dataObject;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup ctx) {
        PythonProject project = ctx.lookup(PythonProject.class);
        if (project == null) {
            DataObject dataObj = ctx.lookup(DataObject.class);
            if (dataObj != null && dataObj.getPrimaryFile().isFolder()
                    && FileOwnerQuery.getOwner(dataObj.getPrimaryFile()) == null) {
                return new PythonPoetryMenuAction(dataObj, true);
            }
            return this;
        }
        if (PythonUtility.isPoetry(project)) {
            Map poetryScr = null;
            try (FileConfig conf = FileConfig.of(FileUtil.toFile(project.getProjectDirectory()
                    .getFileObject("pyproject.toml")))) {
                conf.load();
                com.electronwill.nightconfig.core.Config poetryScriptsConf = conf.get("tool.poetry.scripts");

                if (poetryScriptsConf != null) {
                    poetryScr = poetryScriptsConf.valueMap();
                }

            }
            return new PythonPoetryMenuAction(project, true, poetryScr);
        }

        return this;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        JMenu main = new JMenu(Bundle.CTL_PythonPoetryMenuAction());
        JMenu scriptsMenu = new JMenu(Bundle.CTL_PythonPoetryRunScriptsMenu());

        if (p != null) {
            main.add(new PythonPoetryInstall(p));
            if (poetryScripts != null) {
                poetryScripts.forEach((t, u) -> {
                    scriptsMenu.add(new PythonPoetryRunScript(p, t.toString()));
                });
                main.add(scriptsMenu);
            }
            main.add(new PythonPoetryShow(p));
            main.add(new PythonPoetryShell(p));

        } else {
            main.add(new PythonPoetryInit(dataObject));
        }
        return isEnabled() ? main : null;
    }

}
