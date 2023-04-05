package org.netbeans.modules.python.packagemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import javax.swing.DefaultListModel;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import kong.unirest.Unirest;
import org.javatuples.Quartet;
import org.javatuples.Triplet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.netbeans.api.keyring.Keyring;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.swing.outline.DefaultOutlineModel;
import org.netbeans.swing.outline.OutlineModel;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonPackagesModel {

    static HashMap<String, String> installedMap;
    static String[] userRepos;
    static TreeModel model;
    static DefaultMutableTreeNode installedPackages;

    public static OutlineModel getModel() {
        DefaultMutableTreeNode root = new PythonRootTreeNode(Triplet.with("Packages", "", ""));
        //create the Default child nodes
        installedPackages = new DefaultMutableTreeNode(Triplet.with("Installed", "", ""));
        DefaultMutableTreeNode pyPi = new PythonRootTreeNode(Triplet.with("PyPI", "", ""));

        //add the child nodes
        root.add(installedPackages);
        root.add(pyPi);

        installedPackages.add(addLoadingNode());
        pyPi.add(addLoadingNode());

        addUserRepos(root);

        //create the tree by passing in the root node
        JTree tree = new JTree(root);
        model = tree.getModel();

        OutlineModel mdl = DefaultOutlineModel.createOutlineModel(model,
                new PythonRowModel(), true, "Package");

        return mdl;
    }

    private static DefaultMutableTreeNode addLoadingNode() {
        return new DefaultMutableTreeNode(Triplet.with("Loading", "", ""), false);
    }

    public static void loadInstalled(String pythonPath) throws JSONException {
        ProgressHandle createHandle = ProgressHandle.createHandle("Collecting installed Packages");
        createHandle.setInitialDelay(0);
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                JSONArray installedPackages1 = getInstalledPackages(pythonPath);
                createHandle.start(installedPackages1.length());
                installedPackages.removeAllChildren();
                installedMap = new HashMap<>(installedPackages1.length());
                for (int i = 0; i < installedPackages1.length(); i++) {
                    JSONObject ipackage = installedPackages1.getJSONObject(i);
                    String name = ipackage.getString("name");
                    String version = ipackage.getString("version");
                    createHandle.progress(name + ":" + version, i);
                    installedMap.putIfAbsent(name, version);
                    installedPackages.add(new DefaultMutableTreeNode(Triplet.with(name,
                            version, "")));

                }
                return null;
            }

            @Override
            protected void done() {
                refresh(installedPackages);
                createHandle.finish();
            }

            private JSONArray getInstalledPackages(String pythonPath) {
                try {
                    return new JSONArray(PythonUtility.getCommandOutput(new String[]{pythonPath,
                        "-m",
                        "pip",
                        "list",
                        "--format",
                        "json"}, null));
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return new JSONArray();
            }

        }.execute();

    }

    public static void loadPyPI(DefaultMutableTreeNode pyPi) {
        ProgressHandle createHandle = ProgressHandle.createHandle("Collecting PyPI indexe");
        createHandle.setInitialDelay(0);
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                Elements pypiPackages = getPypiPackages();
                createHandle.start(pypiPackages.size());
                pyPi.removeAllChildren();
                int count = 0;
                for (Element pypiPackage : pypiPackages) {
                    count++;
                    String text = pypiPackage.text();
                    createHandle.progress(text, count);
                    pyPi.add(new DefaultMutableTreeNode(Triplet.with(text, installedMap
                            .getOrDefault(text, ""),
                            "https://pypi.org/project")));

                }
                return null;
            }

            @Override
            protected void done() {
                refresh(pyPi);
                createHandle.finish();
            }

            private Elements getPypiPackages() {
                kong.unirest.HttpResponse<String> response = Unirest.get("https://pypi.org/simple")
                        .asString();
                Document parse = Jsoup.parse(response.getBody(), Parser.htmlParser());
                Elements select = parse.select("body > a");
                return select;
            }

        }.execute();
    }

    static void collapse(DefaultMutableTreeNode node) {
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                node.removeAllChildren();
                node.add(addLoadingNode());
                return null;
            }

            @Override
            protected void done() {
                refresh(node);
            }

        }.execute();
    }

    private static void refresh(DefaultMutableTreeNode node) {
        ((DefaultTreeModel) model).nodeStructureChanged(node);
    }

    private static void addUserRepos(DefaultMutableTreeNode root) {
        try {
            File file = PythonUtility.REPOS;
            if (!file.exists()) {
                return;
            }
            JSONArray jsonArray = new JSONArray(Files.readString(file.toPath()));
            DefaultListModel<Quartet<String, String, String, String>> listModel = new DefaultListModel<>();
            userRepos = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String url = jsonObject.getString("url");
                String login = jsonObject.getString("login");
                char[] pass = Keyring.read(login);
                DefaultMutableTreeNode defaultMutableTreeNode = new PythonRootTreeNode(Triplet.with(name,
                        login + "@" + (pass != null ? pass : ""), url));
                defaultMutableTreeNode.add(addLoadingNode());
                root.add(defaultMutableTreeNode);
                userRepos[i] = name;
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    static void loadUserRepos(DefaultMutableTreeNode node) {
        Triplet triplet = (Triplet) node.getUserObject();
        String url = triplet.getValue2().toString().strip();
        String[] cred = triplet.getValue1().toString().split("@");
        String login = cred[0];
        String pass = cred[1];

        ProgressHandle createHandle = ProgressHandle.createHandle("Collecting " + triplet.getValue0().toString() + " indexe");
        createHandle.setInitialDelay(0);
        new SwingWorker<>() {
            @Override
            protected Object doInBackground() throws Exception {
                Elements repoPackages = getRepoPackages();
                createHandle.start(repoPackages.size());
                node.removeAllChildren();
                int count = 0;
                for (Element repoPackage : repoPackages) {
                    count++;
                    String text = repoPackage.text();
                    createHandle.progress(text, count);
                    node.add(new DefaultMutableTreeNode(Triplet.with(text, installedMap
                            .getOrDefault(text, ""),
                            url)));

                }
                return null;
            }

            @Override
            protected void done() {
                refresh(node);
                createHandle.finish();
            }

            private Elements getRepoPackages() {
                kong.unirest.HttpResponse<String> response = Unirest
                        .get(url)
                        .basicAuth(login, pass)
                        .asString();
                Document parse = Jsoup.parse(response.getBody(), Parser.htmlParser());
                Elements select = parse.select("body > a");
                return select;
            }

        }.execute();
    }
}
