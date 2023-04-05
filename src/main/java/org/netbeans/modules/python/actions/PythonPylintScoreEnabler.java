package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.modules.python.source.PythonPylintScoreBar;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "org.netbeans.modules.python.actions.PythonPylintScoreEnabler"
)
@ActionRegistration(
        displayName = "#CTL_PylintScore"
)
@ActionReference(path = "Editors/text/x-python/Popup", position = 905)
@Messages("CTL_PylintScore=Pylint Code Score")
public final class PythonPylintScoreEnabler implements ActionListener {

    public PythonPylintScoreEnabler(EditorCookie context) {
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        for (JTextComponent target : EditorRegistry.componentList()) {
            PythonPylintScoreBar sb = PythonPylintScoreBar.getSideBar(target);
            if (sb != null) {
                sb.showScorePanel(true);
            }
        }

    }
}
