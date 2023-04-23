package org.netbeans.modules.python.debugger.views;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import org.netbeans.api.debugger.Properties;
import org.netbeans.modules.python.debugger.PythonDebugger;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.spi.debugger.DebuggerServiceRegistration;
import org.netbeans.spi.debugger.ui.EngineComponentsProvider;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@DebuggerServiceRegistration(path = PythonDebugger.PYTHON_SESSION, types = EngineComponentsProvider.class)
public class PythonDebugEngineComponentsProvider implements EngineComponentsProvider {

    private static final String PROPERTY_CLOSED_TC = "closedTopComponents";
    private static final String PROPERTY_MINIMIZED_TC = "minimizedTopComponents";
    private static final String PROPERTY_BASE_NAME = "PYthonSession.EngineComponentsProvider";

    private static final String[] DBG_COMPONENTS_OPENED = {
        /*"localsView", "watchesView",*/"breakpointsView", "callstackView", "sessionsView"
    };
    private static final String[] DBG_COMPONENTS_CLOSED = { /*"debuggingView", "evaluatorPane", "resultsView"*/};

    @Override
    public List<ComponentInfo> getComponents() {
        List<ComponentInfo> components = new ArrayList<>(DBG_COMPONENTS_OPENED.length
                + DBG_COMPONENTS_CLOSED.length);
        for (String cid : DBG_COMPONENTS_OPENED) {
            components.add(EngineComponentsProvider.ComponentInfo.create(
                    cid, isOpened(cid, true), isMinimized(cid)));
        }
        for (String cid : DBG_COMPONENTS_CLOSED) {
            components.add(EngineComponentsProvider.ComponentInfo.create(
                    cid, isOpened(cid, false), isMinimized(cid)));
        }
        return components;
    }

    private static boolean isOpened(String cid, boolean open) {
        if (cid.equals("watchesView")) {
            Preferences preferences = NbPreferences.forModule(ContextProvider.class)
                    .node("variables_view");
            open = !preferences.getBoolean("show_watches", true);
        }
        boolean wasClosed = Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                getProperties(PROPERTY_CLOSED_TC).getBoolean(cid, false);
        boolean wasOpened = !Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                getProperties(PROPERTY_CLOSED_TC).getBoolean(cid, true);
        open = (open && !wasClosed || !open && wasOpened);
        return open;
    }

    private static boolean isMinimized(String cid) {
        boolean wasMinimized = Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                getProperties(PROPERTY_MINIMIZED_TC).getBoolean(cid, false);
        boolean wasDeminim = !Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                getProperties(PROPERTY_MINIMIZED_TC).getBoolean(cid, false);
        boolean minimized = (wasMinimized || !wasDeminim);
        return minimized;
    }

    @Override
    public void willCloseNotify(List<ComponentInfo> components) {
        for (ComponentInfo ci : components) {
            Component c = ci.getComponent();
            if (c instanceof TopComponent) {
                TopComponent tc = (TopComponent) c;
                boolean isOpened = tc.isOpened();
                String tcId = WindowManager.getDefault().findTopComponentID(tc);
                Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                        getProperties(PROPERTY_CLOSED_TC).setBoolean(tcId, !isOpened);
                boolean isMinimized = WindowManager.getDefault().isTopComponentMinimized(tc);
                Properties.getDefault().getProperties(PROPERTY_BASE_NAME).
                        getProperties(PROPERTY_MINIMIZED_TC).setBoolean(tcId, isMinimized);
            }
        }
    }

}
