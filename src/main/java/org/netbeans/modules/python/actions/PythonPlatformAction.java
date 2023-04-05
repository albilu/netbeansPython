package org.netbeans.modules.python.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Tools",
        id = "org.netbeans.modules.python.actions.PythonPlatformAction"
)
@ActionRegistration(
        displayName = "#CTL_PythonPlatformAction"
)
@ActionReference(path = "Menu/Tools", position = 250)

@Messages("CTL_PythonPlatformAction=Python Platforms")
public final class PythonPlatformAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        OptionsDisplayer.getDefault().open("PythonOptions/PyPlatform");
    }
}
