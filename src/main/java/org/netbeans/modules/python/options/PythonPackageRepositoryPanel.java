package org.netbeans.modules.python.options;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.javatuples.Quartet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.keyring.Keyring;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.packagemanager.PythonPackageManagerTopComponent;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

final class PythonPackageRepositoryPanel extends javax.swing.JPanel {

    private final PythonPackageRepositoryOptionsPanelController controller;
    File file;

    PythonPackageRepositoryPanel(PythonPackageRepositoryOptionsPanelController controller) {
        this.controller = controller;
        initComponents();

        repoList.setCellRenderer(new PythonPackageRepoRenderer());
        file = PythonUtility.REPOS;
        if (!file.exists()) {
            try {
                file.createNewFile();
                Files.writeString(file.toPath(), "[]");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        repoSplitPane = new javax.swing.JSplitPane();
        repoListPanel = new javax.swing.JPanel();
        repoListScrollPane = new javax.swing.JScrollPane();
        repoList = new javax.swing.JList<>();
        repoInfosPanel = new javax.swing.JPanel();
        nameLabel = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        urlLabel = new javax.swing.JLabel();
        urlTextField = new javax.swing.JTextField();
        loginLabel = new javax.swing.JLabel();
        passLabel = new javax.swing.JLabel();
        loginTextField = new javax.swing.JTextField();
        passPasswordField = new javax.swing.JPasswordField();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        updateButton = new javax.swing.JButton();

        repoListPanel.setLayout(new java.awt.BorderLayout());

        repoList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        repoList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                repoListValueChanged(evt);
            }
        });
        repoListScrollPane.setViewportView(repoList);

        repoListPanel.add(repoListScrollPane, java.awt.BorderLayout.CENTER);

        repoSplitPane.setLeftComponent(repoListPanel);

        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.nameLabel.text")); // NOI18N

        nameTextField.setText(org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.nameTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(urlLabel, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.urlLabel.text")); // NOI18N

        urlTextField.setText(org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.urlTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(loginLabel, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.loginLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(passLabel, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.passLabel.text")); // NOI18N

        loginTextField.setText(org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.loginTextField.text")); // NOI18N

        passPasswordField.setText(org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.passPasswordField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(addButton, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.addButton.text")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.removeButton.text")); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(updateButton, org.openide.util.NbBundle.getMessage(PythonPackageRepositoryPanel.class, "PythonPackageRepositoryPanel.updateButton.text")); // NOI18N
        updateButton.setEnabled(false);
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout repoInfosPanelLayout = new javax.swing.GroupLayout(repoInfosPanel);
        repoInfosPanel.setLayout(repoInfosPanelLayout);
        repoInfosPanelLayout.setHorizontalGroup(
            repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(repoInfosPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(repoInfosPanelLayout.createSequentialGroup()
                        .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameLabel)
                            .addComponent(urlLabel)
                            .addComponent(loginLabel)
                            .addComponent(passLabel))
                        .addGap(18, 18, 18)
                        .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameTextField)
                            .addComponent(urlTextField)
                            .addComponent(loginTextField)
                            .addComponent(passPasswordField)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, repoInfosPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(updateButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)))
                .addContainerGap())
        );
        repoInfosPanelLayout.setVerticalGroup(
            repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(repoInfosPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(urlLabel)
                    .addComponent(urlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(loginLabel)
                    .addComponent(loginTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passLabel)
                    .addComponent(passPasswordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(repoInfosPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addButton)
                    .addComponent(removeButton)
                    .addComponent(updateButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        repoSplitPane.setRightComponent(repoInfosPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 734, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(repoSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(repoSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        String name = nameTextField.getText();
        String url = urlTextField.getText();
        String login = loginTextField.getText();
        char[] pass = passPasswordField.getPassword();
        if (!name.isEmpty() && !url.isEmpty()) {
            DefaultListModel<Quartet<String, String, String, String>> listModel = (DefaultListModel) repoList.getModel();
            listModel.addElement(new Quartet<>(name, url, login, new String(pass)));
            repoList.setModel(listModel);
        }

    }//GEN-LAST:event_addButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        DefaultListModel model = (DefaultListModel) repoList.getModel();
        int selectedIndex = repoList.getSelectedIndex();
        if (selectedIndex != -1) {
            model.remove(selectedIndex);
        }

    }//GEN-LAST:event_removeButtonActionPerformed

    private void repoListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_repoListValueChanged
        if (repoList.getSelectedIndex() != -1) {
            updateButton.setEnabled(true);
            Quartet<String, String, String, String> repo = (Quartet) repoList.getSelectedValue();
            nameTextField.setText(repo.getValue0());
            urlTextField.setText(repo.getValue1());
            loginTextField.setText(repo.getValue2());
            passPasswordField.setText(repo.getValue3());

        }
    }//GEN-LAST:event_repoListValueChanged

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        DefaultListModel model = (DefaultListModel) repoList.getModel();
        int selectedIndex = repoList.getSelectedIndex();
        String name = nameTextField.getText();
        String url = urlTextField.getText();
        if (selectedIndex != -1 && !name.isEmpty() && !url.isEmpty()) {
            model.set(selectedIndex, new Quartet<>(name,
                    url, loginTextField.getText(), new String(passPasswordField.getPassword())));
        }
    }//GEN-LAST:event_updateButtonActionPerformed

    void load() {
        try {
            // Example:
            // someCheckBox.setSelected(Preferences.userNodeForPackage(PythonPackageRepositoryPanel.class).getBoolean("someFlag", false));
            // or for org.openide.util with API spec. version >= 7.4:
            // someCheckBox.setSelected(NbPreferences.forModule(PythonPackageRepositoryPanel.class).getBoolean("someFlag", false));
            // or:
            // someTextField.setText(SomeSystemOption.getDefault().getSomeStringProperty());
            JSONArray jsonArray = new JSONArray(Files.readString(file.toPath()));
//            DefaultListModel<Quartet<String, String, String, String>> listModel = new DefaultListModel<>();
            ListDataListener dl = new ListDataListener() {
                @Override
                public void intervalAdded(ListDataEvent e) {
                    controller.changed();
                }

                @Override
                public void intervalRemoved(ListDataEvent e) {
                    controller.changed();
                }

                @Override
                public void contentsChanged(ListDataEvent e) {
                    controller.changed();
                }

            };
            DefaultListModel<Quartet<String, String, String, String>> listModel = new DefaultListModel<>();
            listModel.addListDataListener(dl);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String login = jsonObject.getString("login");
                char[] read = Keyring.read(login);
                listModel.addElement(new Quartet<>(jsonObject.getString("name"),
                        jsonObject.getString("url"), login,
                        login.isEmpty() ? "" : (read != null ? new String(read) : "")));
            }
            repoList.setModel(listModel);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    void store() {
        // Example:
        // Preferences.userNodeForPackage(PythonPackageRepositoryPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or for org.openide.util with API spec. version >= 7.4:
        // NbPreferences.forModule(PythonPackageRepositoryPanel.class).putBoolean("someFlag", someCheckBox.isSelected());
        // or:
        // SomeSystemOption.getDefault().setSomeStringProperty(someTextField.getText());
        if (controller.isChanged()) {
            DefaultListModel<Quartet<String, String, String, String>> model = (DefaultListModel) repoList.getModel();
            int size = model.getSize();
            JSONArray jsonArray = new JSONArray(size);
            Map<String, String> hashMap = new HashMap<>();
            for (int i = 0; i < size; i++) {
                Quartet<String, String, String, String> elementAt = model.getElementAt(i);
                hashMap.put("name", elementAt.getValue0());
                hashMap.put("url", elementAt.getValue1());
                hashMap.put("login", elementAt.getValue2());
                //hashMap.put("pass", elementAt.getValue3());
                Keyring.save(elementAt.getValue2(), elementAt.getValue3().toCharArray(), "Repo pass");
                jsonArray.put(new JSONObject(hashMap));

            }
            try {
                Files.writeString(file.toPath(), jsonArray.toString());
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            EventQueue.invokeLater(() -> {
                TopComponent findTopComponent = WindowManager.getDefault().findTopComponent("PythonPackageManagerTopComponent");
                if (findTopComponent != null && findTopComponent.isOpened()) {
                    PythonPackageManagerTopComponent.refresh();
                }

            });
        }
    }

    boolean valid() {
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JLabel loginLabel;
    private javax.swing.JTextField loginTextField;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField nameTextField;
    private javax.swing.JLabel passLabel;
    private javax.swing.JPasswordField passPasswordField;
    private javax.swing.JButton removeButton;
    private javax.swing.JPanel repoInfosPanel;
    private javax.swing.JList<Quartet<String,String,String,String>> repoList;
    private javax.swing.JPanel repoListPanel;
    private javax.swing.JScrollPane repoListScrollPane;
    private javax.swing.JSplitPane repoSplitPane;
    private javax.swing.JButton updateButton;
    private javax.swing.JLabel urlLabel;
    private javax.swing.JTextField urlTextField;
    // End of variables declaration//GEN-END:variables
}
