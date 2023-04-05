package org.netbeans.modules.python.actions;

/**
 *
 * @author albilu
 */
import com.jediterm.pty.PtyProcessTtyConnector;
import com.pty4j.PtyProcess;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import org.netbeans.modules.python.repl.IPythonTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@ActionID(
        category = "Edit",
        id = "org.netbeans.modules.python.actions.RunSelectionAction"
)
@ActionRegistration(
        displayName = "#CTL_RunSelectionAction", lazy = false
)
@ActionReference(path = "Editors/text/x-python/Popup", position = 5)
@NbBundle.Messages({
    "CTL_RunSelectionAction=Run Selection",
    "CTL_RunSelectionNoSelection=Nothing selected"
})
public final class PythonRunSelectionAction extends AbstractAction implements LookupListener, ContextAwareAction {

    private static final long serialVersionUID = 1L;

    private Lookup context;
    Lookup.Result<EditorCookie> lkpInfo;
    static TopComponent findTopComponent;
    static PtyProcessTtyConnector tty;
    static byte[] enterKey;

    public PythonRunSelectionAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PythonRunSelectionAction(Lookup context) {
        putValue(Action.NAME, Bundle.CTL_RunSelectionAction());
        this.context = context;
    }

    void init() {
        assert SwingUtilities.isEventDispatchThread() : "this shall be called just from AWT thread";
        if (lkpInfo != null) {
            return;
        }
        //The thing we want to listen for the presence or absence of
        //on the global selection
        lkpInfo = context.lookupResult(EditorCookie.class);
        lkpInfo.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        if (lkpInfo.allInstances().isEmpty()) {
            setEnabled(false);
        } else {
            try {
                JEditorPane jep = getJEP();
                if (jep != null && jep.getSelectedText() != null && !jep.getSelectedText().isEmpty()) {
                    setEnabled(true);
                } else {
                    setEnabled(false);
                }
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PythonRunSelectionAction(context);
    }

    @Override
    public boolean isEnabled() {
        init();
        return super.isEnabled();
    }

    private JEditorPane getJEP() throws DataObjectNotFoundException {
        EditorCookie editorCookie = getEC();
        JEditorPane jep = NbDocument.findRecentEditorPane(editorCookie);
        if (jep == null && editorCookie.getOpenedPanes() != null) {
            jep = editorCookie.getOpenedPanes()[0];
        }
        return jep;
    }

    private EditorCookie getEC() {
        return lkpInfo.allInstances()
                .stream().findFirst().get();
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        init();
        EditorCookie ec = getEC();
        JEditorPane openedPane = ec.getOpenedPanes()[0];
        if (openedPane != null) {
            String selectedText = openedPane.getSelectedText();
            if (selectedText != null && !selectedText.isEmpty()) {
                if (findTopComponent == null) {
                    findTopComponent = WindowManager.getDefault().findTopComponent("IPythonTopComponent");
                }
                if (findTopComponent != null && !findTopComponent.isOpened()) {
                    findTopComponent.open();
                }
                findTopComponent.requestActive();
                WindowManager.getDefault().invokeWhenUIReady(() -> {
                    try {
                        tty = (PtyProcessTtyConnector) ((IPythonTopComponent) findTopComponent).ttyConnector;
                        enterKey = new byte[]{((PtyProcess) tty.getProcess()).getEnterKeyCode()};
                        tty.write(selectedText);
                        //FIXME-EXTERNAL: Ipython seems to need Shift+Enter to execute the code same issue
                        //discussed on vscode and spyder IDE
                        //https://github.com/microsoft/vscode-python/issues/17172
                        //https://github.com/spyder-ide/spyder/issues/2696
                        tty.write(enterKey);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
            } else {
                StatusDisplayer.getDefault().setStatusText(Bundle.CTL_RunSelectionNoSelection());
            }
        }
    }

}
