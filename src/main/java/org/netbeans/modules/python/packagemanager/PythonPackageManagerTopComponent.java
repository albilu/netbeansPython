package org.netbeans.modules.python.packagemanager;

import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.regex.Matcher;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.tree.DefaultMutableTreeNode;
import org.apache.commons.lang3.StringUtils;
import org.javatuples.Triplet;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.statusbar.PythonStatusBarPanel;
import org.netbeans.swing.etable.QuickFilter;
import org.netbeans.swing.outline.Outline;
import org.netbeans.swing.outline.OutlineModel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.QuickSearch;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.netbeans.modules.python.packagemanager//PythonPackageManager//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "PythonPackageManagerTopComponent",
        iconBase = "org/netbeans/modules/python/pip.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "org.netbeans.modules.python.packagemanager.PythonPackageManagerTopComponent")
@ActionReference(path = "Menu/Window", position = 1250)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PythonPackageManagerAction",
        preferredID = "PythonPackageManagerTopComponent"
)
@Messages({
    "CTL_PythonPackageManagerAction=Python Packages Manager",
    "CTL_PythonPackageManagerTopComponent=Python Packages",
    "HINT_PythonPackageManagerTopComponent=This is a Python Packages Manager window"
})
//On package manager if project is poetry use poetry command to add and remove packages
//in package manager if python_path contains poetry=> isPoetry
//modify install and remove
//reluctant to do so as poetry virtual environment and dependencies management are very different
//plus pycharm is not supporting this feature neither
//TODO-FEATURE => need vote
public final class PythonPackageManagerTopComponent extends TopComponent {

    private static final long serialVersionUID = 1L;
    static Outline outline;
    static QuickSearch attach;
    static String filterText;
    static final JFXPanel jfxPanel = new JFXPanel();
    static WebView webView = null;
    static WebEngine engine = null;
    ProgressHandle pg;

    RequestProcessor RP = new RequestProcessor(this.getClass().getName(), 3);

    public PythonPackageManagerTopComponent() {
        initComponents();
        setName(Bundle.CTL_PythonPackageManagerTopComponent());
        setToolTipText(Bundle.HINT_PythonPackageManagerTopComponent());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        managerSplitPane = new javax.swing.JSplitPane();
        packagesPanel = new javax.swing.JPanel();
        managerToolBar = new javax.swing.JToolBar();
        searchToggleButton = new javax.swing.JToggleButton();
        repositoryButton = new javax.swing.JButton();
        libsPanel = new javax.swing.JPanel();
        webPanel = new javax.swing.JPanel();
        webActionsPanel = new javax.swing.JPanel();
        versionsComboBox = new javax.swing.JComboBox<>();
        installButton = new javax.swing.JButton();
        packageNameLabel = new javax.swing.JLabel();
        browserPanel = new javax.swing.JPanel();

        managerToolBar.setBorder(null);
        managerToolBar.setOrientation(javax.swing.SwingConstants.VERTICAL);
        managerToolBar.setRollover(true);
        managerToolBar.setMaximumSize(new java.awt.Dimension(22, 44));

        searchToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/python/find16.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(searchToggleButton, org.openide.util.NbBundle.getMessage(PythonPackageManagerTopComponent.class, "PythonPackageManagerTopComponent.searchToggleButton.text")); // NOI18N
        searchToggleButton.setFocusable(false);
        searchToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        searchToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        searchToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchToggleButtonActionPerformed(evt);
            }
        });
        managerToolBar.add(searchToggleButton);

        repositoryButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/python/settings.png"))); // NOI18N
        repositoryButton.setToolTipText(org.openide.util.NbBundle.getMessage(PythonPackageManagerTopComponent.class, "PythonPackageManagerTopComponent.repositoryButton.toolTipText")); // NOI18N
        repositoryButton.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        repositoryButton.setFocusable(false);
        repositoryButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        repositoryButton.setText(org.openide.util.NbBundle.getMessage(PythonPackageManagerTopComponent.class, "PythonPackageManagerTopComponent.repositoryButton.label")); // NOI18N
        repositoryButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        repositoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                repositoryButtonActionPerformed(evt);
            }
        });
        managerToolBar.add(repositoryButton);

        libsPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout packagesPanelLayout = new javax.swing.GroupLayout(packagesPanel);
        packagesPanel.setLayout(packagesPanelLayout);
        packagesPanelLayout.setHorizontalGroup(
            packagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(packagesPanelLayout.createSequentialGroup()
                .addComponent(managerToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(libsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        packagesPanelLayout.setVerticalGroup(
            packagesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(managerToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 613, Short.MAX_VALUE)
            .addComponent(libsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        managerSplitPane.setLeftComponent(packagesPanel);

        versionsComboBox.setEnabled(false);

        org.openide.awt.Mnemonics.setLocalizedText(installButton, org.openide.util.NbBundle.getMessage(PythonPackageManagerTopComponent.class, "PythonPackageManagerTopComponent.installButton.text")); // NOI18N
        installButton.setEnabled(false);
        installButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonActionPerformed(evt);
            }
        });

        packageNameLabel.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(packageNameLabel, org.openide.util.NbBundle.getMessage(PythonPackageManagerTopComponent.class, "PythonPackageManagerTopComponent.packageNameLabel.text")); // NOI18N

        javax.swing.GroupLayout webActionsPanelLayout = new javax.swing.GroupLayout(webActionsPanel);
        webActionsPanel.setLayout(webActionsPanelLayout);
        webActionsPanelLayout.setHorizontalGroup(
            webActionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(webActionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(packageNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(versionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(installButton)
                .addContainerGap())
        );
        webActionsPanelLayout.setVerticalGroup(
            webActionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(webActionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(webActionsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(versionsComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(installButton)
                    .addComponent(packageNameLabel))
                .addContainerGap())
        );

        browserPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout webPanelLayout = new javax.swing.GroupLayout(webPanel);
        webPanel.setLayout(webPanelLayout);
        webPanelLayout.setHorizontalGroup(
            webPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(webPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(webActionsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(webPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, webPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(browserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        webPanelLayout.setVerticalGroup(
            webPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(webPanelLayout.createSequentialGroup()
                .addComponent(webActionsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
            .addGroup(webPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, webPanelLayout.createSequentialGroup()
                    .addGap(44, 44, 44)
                    .addComponent(browserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        managerSplitPane.setRightComponent(webPanel);

        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents

    private void repositoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_repositoryButtonActionPerformed
        OptionsDisplayer.getDefault().open("PythonOptions/PyRepo");
    }//GEN-LAST:event_repositoryButtonActionPerformed

    private void installButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_installButtonActionPerformed
        try {
            String text = installButton.getText();
            String pyPath = PythonStatusBarPanel.currentPyPath.isEmpty()
                    ? PythonUtility.getPlatformPythonExe() : PythonStatusBarPanel.currentPyPath;
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) outline
                    .getValueAt(outline.getSelectedRow(), 0);
            Triplet triNode = (Triplet) node.getUserObject();
            String name = triNode.getValue0().toString();
            switch (text) {
                case "Install":
                    RP.post(() -> {
                        PythonUtility.processExecutor(
                                new String[]{
                                    pyPath,
                                    "-m",
                                    "pip",
                                    "install",
                                    String.format(
                                            "%s%s",
                                            name,
                                            versionsComboBox.getSelectedItem() != null
                                            ? "==" + versionsComboBox.getSelectedItem().toString()
                                            : "")
                                },
                                "Installing Package " + name
                        );
                        PythonPackagesModel.loadInstalled(pyPath);
                    });
                    break;
                case "Delete":
                    RP.post(() -> {
                        PythonUtility.processExecutor(new String[]{pyPath, "-m", "pip",
                            "uninstall", "-y", name}, "Deleting Package " + name);
                        PythonPackagesModel.loadInstalled(pyPath);
                    });
                    break;
                default:
                    throw new AssertionError();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_installButtonActionPerformed

    private void searchToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchToggleButtonActionPerformed
        setSearchVisible(searchToggleButton.isSelected());
    }//GEN-LAST:event_searchToggleButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel browserPanel;
    private javax.swing.JButton installButton;
    private javax.swing.JPanel libsPanel;
    private javax.swing.JSplitPane managerSplitPane;
    private javax.swing.JToolBar managerToolBar;
    private javax.swing.JLabel packageNameLabel;
    private javax.swing.JPanel packagesPanel;
    private javax.swing.JButton repositoryButton;
    private javax.swing.JToggleButton searchToggleButton;
    private javax.swing.JComboBox<String> versionsComboBox;
    private javax.swing.JPanel webActionsPanel;
    private javax.swing.JPanel webPanel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        installButton.setVisible(false);
        versionsComboBox.setVisible(false);

        OutlineModel model = PythonPackagesModel.getModel();
        buildOutline(model);

        JPanel pnlSearch = new JPanel(new GridBagLayout());
        attach = QuickSearch.attach(pnlSearch, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0), new QuickFilterCallback(), true);
        pnlSearch.add(new JLabel(), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        this.add(pnlSearch, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
                GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        this.add(managerSplitPane, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        browserPanel.add(jfxPanel);

        Platform.setImplicitExit(false);

        Platform.runLater(() -> {
            webView = new WebView();
            engine = webView.getEngine();
            engine.setJavaScriptEnabled(true);
            engine.getLoadWorker().totalWorkProperty().addListener((ObservableValue<? extends Number> ov, Number oldState, Number newState) -> {
                if (newState.intValue() >= 0) {
                    pg = ProgressHandle.createHandle("Loading Web Page");
                    pg.setInitialDelay(0);
                    pg.setDisplayName(engine.getLoadWorker().getTitle());
                    pg.start(newState.intValue());
                }
            });
            engine.getLoadWorker().workDoneProperty().addListener((ObservableValue<? extends Number> ov, Number oldState, Number newState) -> {
                pg.progress(engine.getLoadWorker().getMessage(), newState.intValue() >= 0 ? newState.intValue() : 0);
            });

            engine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
                if (newState == Worker.State.CANCELLED || newState == Worker.State.FAILED
                        || newState == Worker.State.SUCCEEDED) {
                    pg.finish();
                }
            });

            Scene scene = new Scene(webView);
            jfxPanel.setScene(scene);
        });

    }

    public static void refresh() {
        OutlineModel model = PythonPackagesModel.getModel();
        outline.setModel(model);
        outline.getOutlineModel().getTreePathSupport().addTreeWillExpandListener(new PythonTreeWillExpand());

    }

    void setSearchVisible(boolean visible) {
        attach.setAlwaysShown(visible);
        if (visible != searchToggleButton.isSelected()) {
            searchToggleButton.setSelected(visible);
        }
    }

    private void buildOutline(OutlineModel model) {
        outline = new Outline();
        outline.setRootVisible(false);
        outline.setModel(model);
        outline.setRowSorter(null);
        outline.setRenderDataProvider(new PythonDataProvider());
        outline.getTableHeader().setDefaultRenderer(new PythonHeaderRenderer());
        outline.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        outline.getOutlineModel().getTreePathSupport().addTreeWillExpandListener(new PythonTreeWillExpand());
        outline.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) outline
                        .getValueAt(outline.getSelectedRow(), 0);
                if (node == null) {
                    return;
                }
                Triplet triNode = (Triplet) node.getUserObject();
                String name = triNode.getValue0().toString();
                String version = triNode.getValue1().toString();
                String index = triNode.getValue2().toString();
                packageNameLabel.setText(name);
                try {
                    openDoc(getUrl(triNode));
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (isRoot(name)) {
                    installButton.setVisible(false);
                    versionsComboBox.setVisible(false);
                    installButton.setEnabled(false);
                    versionsComboBox.setEnabled(false);
                    return;
                }
                installButton.setVisible(true);
                versionsComboBox.setVisible(true);
                installButton.setEnabled(true);
                versionsComboBox.setEnabled(true);
                if (!version.isEmpty()) {
                    installButton.setText("Delete");
                    versionsComboBox.removeAllItems();
                    versionsComboBox.addItem(version);
                    versionsComboBox.setEnabled(false);
                } else {
                    try {
                        installButton.setText("Install");
                        versionsComboBox.removeAllItems();
                        String[] cmd = null;
                        if (index.isEmpty()) {
                            cmd = new String[]{PythonUtility.getLspPythonExe(),
                                "-m",
                                "pip",
                                "index",
                                "versions",
                                name
                            };
                        } else if (index.equals("https://pypi.org/project")) {
                            cmd = new String[]{PythonUtility.getLspPythonExe(),
                                "-m",
                                "pip",
                                "index",
                                "-i",
                                "https://pypi.org/simple",
                                "versions",
                                name
                            };

                        } else {
                            cmd = new String[]{PythonUtility.getLspPythonExe(),
                                "-m",
                                "pip",
                                "index",
                                "-i",
                                index,
                                "versions",
                                name
                            };

                        }
                        String commandOutput = PythonUtility.getCommandOutput(cmd, null);
                        Matcher matcher = PythonUtility.PAC_VERSIONS.matcher(commandOutput);
                        if (matcher.find()) {
                            String[] split = matcher.group(1).split(",");
                            for (String string : split) {
                                versionsComboBox.addItem(string);
                            }
                        }

                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }

                }

            }
        }
        );
        outline.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                attach.processKeyEvent(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

        });
        libsPanel.removeAll();

        libsPanel.add(new JScrollPane(outline));
    }

    @Override
    public void componentClosed() {
        attach.detach();
    }

    private void openDoc(String url) {

        Platform.runLater(() -> {
            if (url.contains("http")) {
                engine.load(url);
            } else {
                engine.loadContent(url);
            }
        });

    }

    private String getUrl(Triplet object) throws IOException {
        String url = object.getValue2().toString();
        String name = object.getValue0().toString();
        if (url.isEmpty() && isRoot(name)) {
            return noDocFound();
        } else if (url.isEmpty()) {
            String commandOutput = PythonUtility.getCommandOutput(new String[]{PythonStatusBarPanel.currentPyPath.isEmpty()
                ? PythonUtility.getPlatformPythonExe() : PythonStatusBarPanel.currentPyPath,
                "-m",
                "pip",
                "show",
                name
            }, null);
            Matcher matcher = PythonUtility.HOME_PAGE.matcher(commandOutput);
            if (matcher.find()) {
                return matcher.group(1).strip();
            }
            return noDocFound();

        }
        return object.getValue2().toString() + "/" + name;
    }

    private static String noDocFound() {
        return "<html><body>No documentation found in current repository</body></html>";
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        //  store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        //  read your settings according to their version

    }

    private class QuickFilterCallback implements QuickSearch.Callback {

        @Override
        public void quickSearchUpdate(String searchText) {
            if (attach != null && !attach.isAlwaysShown()) {
                setSearchVisible(true);
            }
            filterText = searchText;
            EventQueue.invokeLater(() -> {
                outline.setQuickFilter(0, filter);
            });
        }

        @Override
        public void showNextSelection(boolean forward) {
        }

        @Override
        public String findMaxPrefix(String prefix) {
            return prefix;
        }

        @Override
        public void quickSearchConfirmed() {
            EventQueue.invokeLater(() -> {
                outline.requestFocusInWindow();
            });
        }

        @Override
        public void quickSearchCanceled() {
            EventQueue.invokeLater(() -> {
                outline.unsetQuickFilter();
                outline.requestFocusInWindow();
            });
        }

        private final QuickFilter filter = (Object aValue) -> {
            Triplet node = (Triplet) ((DefaultMutableTreeNode) aValue).getUserObject();
            String toString = node.getValue0().toString();
            return toString.contains(filterText)
                    || isRoot(toString);
        };

    }

    private boolean isRoot(String toString) {
        return StringUtils.equalsAny(toString,
                "Installed", "PyPI", "Loading")
                || StringUtils.equalsAny(toString, PythonPackagesModel.userRepos);
    }

}
