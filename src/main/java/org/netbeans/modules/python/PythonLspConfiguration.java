package org.netbeans.modules.python;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.netbeans.api.project.Project;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider.LanguageServerDescription;
import org.netbeans.modules.python.project.PythonProject;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 * Sends pylsp its settings together with the interpreter of the project it
 * serves, so that Jedi completes the packages installed in that interpreter
 * instead of those of the plugin's own pylsp environment (#144).
 */
public final class PythonLspConfiguration {

    private static final Logger LOG = Logger.getLogger(PythonLspConfiguration.class.getName());
    private static final RequestProcessor WORKER = new RequestProcessor(PythonLspConfiguration.class.getName(), 1);
    /** Running Python servers and the project each was started for (null for files outside projects). */
    private static final Map<LSPBindings, Project> SERVERS = new WeakHashMap<>();
    /** Started servers that NetBeans has not initialized yet. */
    private static final Map<LanguageServerDescription, PendingConfiguration> PENDING = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.BIG_DECIMAL).create();
    private static final Type SETTINGS_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    /** Held here because NetBeans keeps its change listeners only weakly. */
    private static final ChangeListener BINDINGS_LISTENER = event -> configurePendingServers();

    static {
        // NetBeans fires this after it has initialized a server and published its bindings.
        LSPBindings.addChangeListener(BINDINGS_LISTENER);
    }

    private PythonLspConfiguration() {
    }

    /** Called by {@link PythonLspClient} for every start and restart of a server. */
    static void serverStarted(Project project, LanguageServerDescription description) {
        PendingConfiguration pending = new PendingConfiguration(project);
        PENDING.put(description, pending);
        Process process = LanguageServerProviderAccessor.getINSTANCE().getProcess(description);
        if (process != null) {
            // Forget servers that exit, also those NetBeans never initialized.
            process.onExit().thenRun(() -> {
                PENDING.remove(description, pending);
                LSPBindings bindings = pending.bindings;
                if (bindings != null) {
                    synchronized (SERVERS) {
                        SERVERS.remove(bindings);
                    }
                }
            });
        }
    }

    static void configurePendingServers() {
        PENDING.forEach((description, pending) -> {
            LSPBindings bindings = LanguageServerProviderAccessor.getINSTANCE().getBindings(description);
            if (bindings != null && PENDING.remove(description, pending)) {
                synchronized (SERVERS) {
                    pending.bindings = bindings;
                    Process process = LanguageServerProviderAccessor.getINSTANCE().getProcess(description);
                    if (process == null || process.isAlive()) {
                        SERVERS.put(bindings, pending.project);
                    }
                }
                WORKER.post(() -> {
                    try {
                        configure(bindings, pending.project, readSettings());
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
            }
        });
    }

    /** Waits until the configuration posted so far has been sent; for tests. */
    static void waitForPendingUpdates() {
        WORKER.post(() -> { }).waitFinished();
    }

    /** Sends the current interpreter to the running servers of a project, after its interpreter changed. */
    public static void configureProject(Project project) {
        WORKER.post(() -> {
            try {
                Map<String, Object> settings = readSettings();
                running().forEach((bindings, server) -> {
                    if (server != null && server.getProjectDirectory().equals(project.getProjectDirectory())) {
                        configure(bindings, server, settings);
                    }
                });
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }

    /** Sends changed global settings to all Python servers, each with its own project's interpreter. */
    public static void configureRunningServers(Map<String, Object> settings) {
        WORKER.post(() -> running().forEach((bindings, project) -> configure(bindings, project, settings)));
    }

    private static Map<LSPBindings, Project> running() {
        synchronized (SERVERS) {
            return new HashMap<>(SERVERS);
        }
    }

    static void configure(LSPBindings bindings, Project project, Map<String, Object> settings) {
        DidChangeConfigurationParams params = configuration(settings, interpreter(project, settings));
        try {
            bindings.getWorkspaceService().didChangeConfiguration(params);
        } catch (RuntimeException ex) {
            // A server that has exited fails here; NetBeans starts and configures a new one when needed.
            LOG.log(Level.FINE, "Cannot configure a Python language server", ex);
            synchronized (SERVERS) {
                SERVERS.remove(bindings);
            }
        }
    }

    /**
     * The interpreter Jedi should use: the one chosen for the project, else a
     * {@code plugins.jedi.environment} set in the settings (returns null to keep
     * it), else the default Python platform. Returns null for an interpreter
     * that does not exist, because pylsp then fails every Jedi request.
     */
    static String interpreter(Project project, Map<String, Object> settings) {
        try {
            String interpreter = projectInterpreter(project);
            if (interpreter == null) {
                if (configuredEnvironment(settings) != null) {
                    return null;
                }
                interpreter = PythonUtility.getPlatformPythonExe();
            }
            if (interpreter == null || interpreter.isBlank()) {
                return null;
            }
            if (!new File(interpreter).isFile()) {
                LOG.log(Level.INFO, "Python interpreter {0} not found; completion uses the language server''s own"
                        + " environment", interpreter);
                return null;
            }
            return interpreter;
        } catch (IOException | RuntimeException ex) {
            // For example a damaged platforms.json: keep pylsp's own environment.
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    /** The interpreter selected for the project, or null if none was selected. */
    static String projectInterpreter(Project project) throws IOException {
        if (project == null) {
            return null;
        }
        FileObject configuration = project.getProjectDirectory().getFileObject("nbproject/project.properties");
        if (configuration != null) {
            Properties properties = new Properties();
            try (InputStream input = configuration.getInputStream()) {
                properties.load(input);
            }
            String interpreter = properties.getProperty("nbproject.python_path");
            if (interpreter != null && !interpreter.isBlank()) {
                return interpreter.strip();
            }
        }
        if (project instanceof PythonProject && PythonUtility.isPoetry((PythonProject) project)) {
            String lspPython = PythonUtility.getLspPythonExe();
            String poetryPython = PythonUtility.getPoetryPythonPath(lspPython, project.getProjectDirectory());
            if (!poetryPython.equals(lspPython)) {
                return poetryPython;
            }
        }
        return null;
    }

    /**
     * The pylsp settings to send: the global settings with
     * {@code plugins.jedi.environment} set to the given interpreter, if any.
     * The global settings are not modified.
     */
    static DidChangeConfigurationParams configuration(Map<String, Object> globalSettings, String interpreter) {
        Map<String, Object> settings = new HashMap<>(globalSettings);
        settings.remove("auto_pop_completion");
        Map<String, Object> plugins = copyMap(settings.get("plugins"));
        Map<String, Object> jedi = copyMap(plugins.get("jedi"));
        if (interpreter != null) {
            jedi.put("environment", interpreter);
        }
        // The default settings have "env_vars": {}. Once an environment is set,
        // Jedi starts its interpreter with exactly these variables, so an empty
        // map would leave it without PATH, HOME or (on Windows) SYSTEMROOT.
        // Without the key pylsp passes on its own environment.
        if (jedi.get("env_vars") instanceof Map && ((Map<?, ?>) jedi.get("env_vars")).isEmpty()) {
            jedi.remove("env_vars");
        }
        if (!jedi.isEmpty()) {
            plugins.put("jedi", jedi);
            settings.put("plugins", plugins);
        }
        return new DidChangeConfigurationParams(Map.of("pylsp", settings));
    }

    private static Object configuredEnvironment(Map<String, Object> settings) {
        Object environment = copyMap(copyMap(settings.get("plugins")).get("jedi")).get("environment");
        return environment instanceof String && !((String) environment).isBlank() ? environment : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyMap(Object value) {
        return value instanceof Map ? new HashMap<>((Map<String, Object>) value) : new HashMap<>();
    }

    static Map<String, Object> readSettings() throws IOException {
        String json;
        if (PythonUtility.SETTINGS.isFile()) {
            json = Files.readString(PythonUtility.SETTINGS.toPath());
        } else {
            try (InputStream input = PythonLspConfiguration.class.getResourceAsStream("settings.json")) {
                if (input == null) {
                    throw new IOException("Missing Python language server settings");
                }
                json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Map<String, Object> settings = GSON.fromJson(json, SETTINGS_TYPE);
        return settings == null ? Map.of() : settings;
    }

    private static final class PendingConfiguration {

        final Project project;
        /** Set once NetBeans has initialized the server; guarded by SERVERS. */
        LSPBindings bindings;

        PendingConfiguration(Project project) {
            this.project = project;
        }
    }
}
