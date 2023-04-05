package org.netbeans.modules.python;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.Maps;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.netbeans.api.extexecution.print.LineConvertors;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.python.options.PythonPlatformManager;
import static org.netbeans.modules.python.options.PythonPlatformManager.getPathFile;
import org.openide.awt.NotificationDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Pair;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonUtility {

    public static final Logger LOG = Logger.getLogger(PythonUtility.class.getName());

    public static final RequestProcessor RP = new RequestProcessor("Retry RP", 2);
    public static final String PYTHON_MIME_TYPE = "text/x-python";
    public static final File PYLSP_VENV_DIR = Paths.get(System.getProperty("netbeans.user")).resolve(".pythonlsp").toFile();

    public static final Pattern HOME_PAGE = Pattern.compile(".*Home-page:\\s+(.*)");
    public static final Pattern PAC_VERSIONS = Pattern.compile(".*Available versions:\\s+(.*)");
    public static final Pattern NO_MODULE_PATTERN = Pattern.compile("(.*):\\s+No module named\\s([a-z-]+)");
    public static final Pattern POETRY_PYTHON_PATH = Pattern.compile("(.*)\\s+\\(Activated\\)");
    static final Pattern PYTHON_STACKTRACE_PATTERN = Pattern.compile("^  File \"(.+\\.py)\", line (\\d+).*");

    public static final LineConvertor HTTP_CONVERTOR = LineConvertors.httpUrl();

    public static LineConvertors.FileLocator FILE_LOCATOR = (String filename) -> {
        FileObject toFileObject = FileUtil.toFileObject(new File(filename.replaceAll("\"", "")
                .strip()));
        return toFileObject != null ? toFileObject : null;
    };

    public static LineConvertor FILE_CONVERTOR = LineConvertors.filePattern(FILE_LOCATOR,
            PYTHON_STACKTRACE_PATTERN,
            null, 1, 2);

    public static final String[] IMPORTANT_FILES = {
        "Dockerfile",
        "Jenkinsfile",
        "LICENSE",
        "md",
        "setup.py",
        "pyproject.toml"
    };

    public static String[] EXCLUDED_DIRS = new String[]{
        ".venv",
        "venv",
        "build",
        "dist",
        "nbproject",
        "py_tests",
        "pynguin-report",
        "pycache",
        ".ropeproject",
        "__pycache__",
        "poetry.lock",
        "coverage.json"
    };

    public static String[] VENV_DEPS = new String[]{
        "pytest",
        "pynguin",
        "build"
    };

    public static String[] LSP_DEPS = new String[]{
        "python-lsp-server[all]",
        "pyls-isort",
        "pylsp-mypy",
        "pylsp-rope",
        "black",
        "python-lsp-black",
        "isort",
        "pyls-memestra",
        "ptpython",
        "ipython",
        "pytest",
        "pynguin",
        "build",
        "pdoc",
        "poetry"
    };

    public static final File SETTINGS = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("settings.json").toFile();
    public static final File REPOS = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("repos.json").toFile();
    public static final File PLATFORMS = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("platforms.json").toFile();
    public static final File ENVS = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("envs.json").toFile();
    public static final File TEST_RUNNER = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("nb_test_runner.py").toFile();
    public static final File TOML_HANDLER = PythonUtility.PYLSP_VENV_DIR.toPath().resolve("toml_handler.py").toFile();

    public static Preferences TERMINAL_PREFS = NbPreferences.root()
            .node("org/netbeans/modules/terminal/nb");

    public static int getTermFontSize() {
        return TERMINAL_PREFS.getInt("term.fontSize", 18);
    }

    public static int geTermFontSt() {
        return TERMINAL_PREFS.getInt("term.fontStyle", 0);
    }

    public static String getTermFontFam() {
        return TERMINAL_PREFS.get("term.fontFamily", "DejaVu Sans Mono");
    }

    public static String getServerVersion() throws IOException {
        return getCommandOutput(new String[]{getLspPythonExe(), "-m", "pylsp", "--version"}, null);
    }

    public static String getCommandOutput(String[] cmd, FileObject projectDir) throws IOException {
        ProcessBuilder p = new ProcessBuilder(cmd);
        if (projectDir != null) {
            p.directory(FileUtil.toFile(projectDir));
        }
        return IOUtils.toString(p.start().getInputStream(), StandardCharsets.UTF_8).strip();
    }

    public static String getPythonStdLibPath(String path) throws IOException {
        return getCommandOutput(new String[]{path, "-c",
            "import sysconfig; print(sysconfig.get_paths()['stdlib'])"}, null);

    }

    public static String getLspPythonExe() {
        return normalizeVenvPath(PYLSP_VENV_DIR.toPath());
    }

    public static String getProjectPythonExe(FileObject fileObject)
            throws IOException {
        if (fileObject != null) {
            Project owner = FileOwnerQuery.getOwner(fileObject);
            if (owner != null) {
                FileObject fileObject1 = owner.getProjectDirectory()
                        .getFileObject("nbproject/project.properties");
                if (fileObject1 != null) {
                    File toFile = FileUtil.toFile(fileObject1);
                    Properties appProps = new Properties();
                    if (toFile != null) {
                        appProps.load(new FileInputStream(toFile));
                        String property = appProps.getProperty("nbproject.python_path");
                        return (property == null || property.isBlank())
                                ? (PythonUtility.isPoetry((PythonProject) owner)
                                /**/ ? PythonUtility.getPoetryPythonPath(PythonUtility.getLspPythonExe(),
                                        owner.getProjectDirectory()) : getPlatformPythonExe())
                                : /*appProps.getProperty("nbproject.python_path")*/ property;
                    }
                }
            }
        }
        return getPlatformPythonExe();
    }

    public static List<Pair<String, String>> getPythonExes() throws IOException {
        List<Pair<String, String>> versions = new ArrayList<>();
        for (String python : new String[]{"python", "python3", "py"}) {
            getCommandOutput(new String[]{Utilities.isWindows()
                ? "where" : "which", python}, null).lines().forEach(exe -> {
                try {
                    String vers = getVersion(exe.strip());
                    if (!vers.isEmpty()) {
                        versions.add(Pair.of(vers, exe.strip()));
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            });

        }

        return versions;
    }

    public static String[] getOsShell() {
        if (Utilities.isWindows()) {
            return new String[]{"cmd.exe", "/c"};
        } else if (Utilities.isMac() || Utilities.isUnix()) {
            return new String[]{getEnvs().getOrDefault("SHELL", "/bin/bash"), "-c"};
        }
        return null;
    }

    public static Map<String, String> getEnvs() {
        Map<String, String> envs = Maps.newHashMap(System.getenv());
        return envs;
    }

    public static String getPlatformPythonExe() throws IOException {
        return PythonPlatformManager.getDefault();
    }

    public static ImageIcon getPythonIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/python-2.png", false);

    }

    public static ImageIcon getClassIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/Classes.png", false);

    }

    public static ImageIcon getMethodIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/methodPublic.png", false);

    }

    public static ImageIcon getPytestIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/pytest.png", false);

    }

    public static ImageIcon getPackageIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/package16.png", false);

    }

    public static ImageIcon getPynguinIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/pynguin.png", false);

    }

    public static Properties getConf(@NonNull Project project) throws IOException {

        Properties conf = new Properties();
        FileObject fileObject = project.getProjectDirectory()
                .getFileObject("nbproject/project.properties");
        File file
                = /*new File(project.getProjectDirectory().getPath()
                + File.separator + "nbproject" + File.separator + "project.properties");*/ Paths.get(project.getProjectDirectory().getPath())
                        .resolve("nbproject").resolve("project.properties").toFile();
        if (fileObject == null) {
            try {
                FileUtils.createParentDirectories(file);
                file.createNewFile();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
        conf.load(new FileInputStream(file));
        return conf;

    }

    public static String getVersion(String projectPythonExe) throws IOException {
        return getCommandOutput(new String[]{projectPythonExe, "--version"}, null);
    }

    public static ImageIcon getErrorIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/error-badge.png", false);

    }

    public static ImageIcon getPythonPackageIcon() {
        return ImageUtilities.loadImageIcon("org/netbeans/modules/python/libraries.png", false);

    }

    public static int venvPackageInstaller(FileObject projectDirectory) {
        String[] cmd = {
            normalizeVenvPath(FileUtil.toFile(projectDirectory).toPath()),
            "-m",
            "pip",
            "install"
        };
        return processExecutor(ArrayUtils.addAll(cmd, VENV_DEPS), "Install packages");
    }

    public static String normalizeVenvPath(Path base) {
        return Utilities.isWindows() ? base.resolve(".venv\\Scripts\\python.exe").toString()
                : base.resolve(".venv/bin/python").toString();

    }

    @NbBundle.Messages("CTL_Install=Installing packages")
    public static void packageInstaller(String errorLline) {
        Matcher matcher = NO_MODULE_PATTERN.matcher(errorLline);
        while (matcher.find()) {
            String[] cmd = {
                matcher.group(1),
                "-m",
                "pip",
                "install",
                matcher.group(2)
            };

            processExecutor(cmd, Bundle.CTL_Install());
        }
    }

    public static int processExecutor(String[] cmd, String message) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmd);
            LOG.info(() -> Arrays.toString(cmd));

            ExecutionDescriptor execDescriptor = new ExecutionDescriptor()
                    .frontWindowOnError(true)
                    .showProgress(true)
                    .outConvertorFactory(new PythonOutputLine())
                    .errConvertorFactory(new PythonOutputLine());

            ExecutionService service = ExecutionService
                    .newService(() -> pb.start(), execDescriptor, message);

            int waitFor = service.run().get();

            return waitFor;
        } catch (InterruptedException | ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return 1;
    }

    @NbBundle.Messages({
        "CTL_CreatePylsp=Create pylsp environment",
        "CTL_PylspInstallIssue=Issue when creating Pylsp environment.",
        "CTL_PylspRetry=Retry"
    })
    public static int installLsp(ClassLoader cl) {
        try {
            if (!PythonUtility.PYLSP_VENV_DIR.exists()) {
                PythonUtility.PYLSP_VENV_DIR.mkdir();
                Files.writeString(SETTINGS.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/settings.json",
                                StandardCharsets.UTF_8, cl)
                );
                Files.writeString(REPOS.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/repos.json",
                                StandardCharsets.UTF_8, cl)
                );
                Files.writeString(PLATFORMS.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/platforms.json",
                                StandardCharsets.UTF_8, cl)
                );
                Files.writeString(ENVS.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/envs.json",
                                StandardCharsets.UTF_8, cl)
                );
                Files.writeString(TEST_RUNNER.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/nb_test_runner.py",
                                StandardCharsets.UTF_8, cl)
                );
                Files.writeString(TOML_HANDLER.toPath(),
                        IOUtils.resourceToString("org/netbeans/modules/python/toml_handler.py",
                                StandardCharsets.UTF_8, cl)
                );
            }
            String[] cmd = {
                PythonUtility.getPlatformPythonExe(),
                "-m",
                "venv",
                PythonUtility.PYLSP_VENV_DIR.toPath().resolve(".venv").toString()
            };

            int processExecutor = PythonUtility.processExecutor(cmd, Bundle.CTL_CreatePylsp());
            if (processExecutor != 0) {

                NotificationDisplayer.getDefault().notify(Bundle.CTL_PylspInstallIssue(),
                        PythonUtility.getErrorIcon(),
                        Bundle.CTL_PylspRetry(),
                        (ActionEvent e) -> {
                            RP.post(() -> {
                                PythonUtility.processExecutor(cmd, Bundle.CTL_CreatePylsp());
                                finish();
                            });
                        },
                        NotificationDisplayer.Priority.HIGH,
                        NotificationDisplayer.Category.ERROR);
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return finish();
    }

    public static String getPipList(String exe) throws IOException {
        return getCommandOutput(new String[]{exe, "-m", "pip", "list"}, null);
    }

    @NbBundle.Messages({
        "CTL_InstallDeps=Install pylsp dependencies",
        "CTL_MissingDeps=Missing dependency:"
    })
    public static int finish() {
        String[] cmd = {PythonUtility.getLspPythonExe(), "-m", "pip", "install", "--upgrade"};
        return processExecutor(ArrayUtils.addAll(cmd, LSP_DEPS), Bundle.CTL_InstallDeps());
    }

    public static void noModuleInstalledHandler(String line) {
        Matcher matcher = PythonUtility.NO_MODULE_PATTERN.matcher(line);
        if (matcher.find()) {
            String pyPackage = matcher.group(2);
            NotificationDisplayer.getDefault().notify(String.format("%s %s", Bundle.CTL_MissingDeps(), pyPackage),
                    PythonUtility.getPythonPackageIcon(),
                    String.format("Install %s", pyPackage),
                    (ActionEvent e) -> {
                        RP.post(() -> {
                            PythonUtility.packageInstaller(line);
                        });
                    },
                    NotificationDisplayer.Priority.NORMAL,
                    NotificationDisplayer.Category.WARNING);
        }
    }

    public static Map getUserEnvs() throws IOException {
        File envsFile = PythonPlatformManager.getPathFile();
        JSONObject jsonObject = new JSONObject(Files.readString(envsFile.toPath()));
        return jsonObject.toMap();
    }

    public static void insertUserEnvs(String toString) {
        try {
            Files.writeString(getPathFile().toPath(), toString);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static ExecutionDescriptor getExecutorDescriptor(ExecutionDescriptor.LineConvertorFactory convertorFactory,
            Runnable preExeRunnable, Runnable postRunnable, boolean controllable) {
        ExecutionDescriptor execDescriptor = new ExecutionDescriptor()
                .frontWindow(true)
                .frontWindowOnError(true)
                .controllable(controllable)
                .showProgress(true)
                .outConvertorFactory(convertorFactory)
                .errConvertorFactory(convertorFactory)
                .preExecution(preExeRunnable)
                .postExecution(postRunnable);

        return execDescriptor;
    }

    public static boolean isRunWithSysEnvs() {
        return NbPreferences.root().getBoolean("sysEnv", true);
    }

    public static void manageRunEnvs(ProcessBuilder pb) {
        try {
            Map<String, String> environment = pb.environment();
            if (!PythonUtility.isRunWithSysEnvs()) {
                environment.clear();
            }
            Map userEnvs = getUserEnvs();
            environment.putAll(userEnvs);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    public static String getVenv(PythonProject get) {
        try {
            return getConf(get).getProperty("nbproject.virtualmanager", "venv");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return "venv";
    }

    public static String getProjectType(File toFile) {
        try (FileConfig conf = FileConfig.of(toFile)) {
            conf.load();
            Object get = conf.get("tool.poetry");
            return get == null ? PythonProject.PYTHON : PythonProject.POETRY;
        }
    }

    public static boolean isPoetry(PythonProject project) {
        FileObject fileObject = project.getProjectDirectory().getFileObject("pyproject.toml");

        return (fileObject != null && PythonUtility.getProjectType(FileUtil.toFile(fileObject))
                .equals(PythonProject.POETRY)) || project.getProjectDirectory().getFileObject("poetry.lock") != null;

    }

    public static String getPoetryPythonPath(String defaultPython, FileObject projectDir) {
        try {
            String[] cmd = {defaultPython, "-m", "poetry", "env", "list", "--full-path"};

            Matcher matcher = PythonUtility.POETRY_PYTHON_PATH.matcher(PythonUtility.getCommandOutput(cmd, projectDir));
            if (matcher.find()) {
                String endPath;
                if (Utilities.isWindows()) {
                    endPath = "\\Scripts\\python.exe";
                } else {
                    endPath = "/bin/python";
                }
                return String.format("%s%s", matcher.group(1), endPath);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return defaultPython;
    }

}
