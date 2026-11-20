package org.netbeans.modules.python;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netbeans.api.project.Project;
import org.netbeans.modules.lsp.client.LSPBindings;
import org.netbeans.modules.lsp.client.LanguageServerProviderAccessor;
import org.netbeans.modules.lsp.client.spi.LanguageServerProvider.LanguageServerDescription;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

import static org.junit.Assert.*;

/** Issue #144: pylsp must complete with the project's interpreter. */
public class PythonLspConfigurationTest {

    /** Message lists of servers that behave as if they had exited. */
    private static final List<List<DidChangeConfigurationParams>> failing = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void configurationSetsTheInterpreterWithoutChangingTheGlobalSettings() {
        Map<String, Object> global = Map.of("auto_pop_completion", true,
                "plugins", Map.of("jedi", Map.of("extra_paths", List.of("shared sources")),
                        "jedi_completion", Map.of("enabled", true)));

        DidChangeConfigurationParams first = PythonLspConfiguration.configuration(global, "/first project/bin/python");
        DidChangeConfigurationParams second = PythonLspConfiguration.configuration(global, "C:\\second project\\python.exe");

        assertEquals("/first project/bin/python", jedi(first).get("environment"));
        assertEquals("C:\\second project\\python.exe", jedi(second).get("environment"));
        assertEquals(List.of("shared sources"), jedi(first).get("extra_paths"));
        assertEquals(Map.of("enabled", true), plugins(first).get("jedi_completion"));
        assertFalse(settings(first).containsKey("auto_pop_completion"));
        assertTrue(global.containsKey("auto_pop_completion"));
        assertFalse(((Map<?, ?>) ((Map<?, ?>) global.get("plugins")).get("jedi")).containsKey("environment"));
    }

    @Test
    public void withoutAnInterpreterTheSettingsAreSentAsBefore() {
        Map<String, Object> global = Map.of("plugins", Map.of("jedi", Map.of("environment", "/user choice/python")));
        assertEquals("/user choice/python", jedi(PythonLspConfiguration.configuration(global, null)).get("environment"));
        assertEquals(Map.of("pylsp", Map.of()), PythonLspConfiguration.configuration(Map.of(), null).getSettings());
    }

    @Test
    public void emptyEnvVarsOfTheDefaultSettingsAreNotSent() throws Exception {
        Map<String, Object> defaults = PythonLspConfiguration.readSettings();
        assertEquals("The bundled settings.json has \"env_vars\": {}", Map.of(),
                ((Map<?, ?>) ((Map<?, ?>) defaults.get("plugins")).get("jedi")).get("env_vars"));
        Map<String, Object> jedi = jedi(PythonLspConfiguration.configuration(defaults, "/project/bin/python"));
        assertFalse("Jedi would start the interpreter with no environment variables", jedi.containsKey("env_vars"));
        assertEquals(List.of("numpy"), jedi.get("auto_import_modules"));

        Map<String, Object> custom = Map.of("plugins", Map.of("jedi", Map.of("env_vars", Map.of("A", "1"))));
        assertEquals(Map.of("A", "1"), jedi(PythonLspConfiguration.configuration(custom, "/p/python")).get("env_vars"));
    }

    @Test
    public void projectInterpreterWinsOverTheSettingsAndDefault() throws Exception {
        String python = interpreterFile("selected café interpreter with spaces");
        withDefaultPlatform(interpreterFile("default"), () -> {
            Map<String, Object> userChoice = Map.of("plugins", Map.of("jedi", Map.of("environment", "/user/python")));
            assertEquals(python, PythonLspConfiguration.interpreter(project(python), userChoice));
            assertEquals(python, PythonLspConfiguration.interpreter(project(python), Map.of()));
        });
    }

    @Test
    public void withoutAProjectInterpreterTheSettingsThenTheDefaultPlatformAreUsed() throws Exception {
        String defaultPython = interpreterFile("default");
        withDefaultPlatform(defaultPython, () -> {
            Map<String, Object> userChoice = Map.of("plugins", Map.of("jedi", Map.of("environment", "/user/python")));
            assertNull("The environment in the settings is kept", PythonLspConfiguration.interpreter(project(""), userChoice));
            assertEquals(defaultPython, PythonLspConfiguration.interpreter(project(""), Map.of()));
            assertEquals("Files outside projects", defaultPython, PythonLspConfiguration.interpreter(null, Map.of()));
            Project withoutProperties = project("");
            withoutProperties.getProjectDirectory().getFileObject("nbproject/project.properties").delete();
            assertEquals(defaultPython, PythonLspConfiguration.interpreter(withoutProperties, Map.of()));
        });
    }

    @Test
    public void missingInterpreterIsNotSentBecauseJediWouldFailEveryRequest() throws Exception {
        String deleted = temporaryFolder.getRoot().toPath().resolve("deleted venv/bin/python").toString();
        withDefaultPlatform(deleted, () -> {
            assertNull(PythonLspConfiguration.interpreter(project(deleted), Map.of()));
            assertNull(PythonLspConfiguration.interpreter(project(""), Map.of()));
            assertNull("A folder is not an interpreter",
                    PythonLspConfiguration.interpreter(project(temporaryFolder.getRoot().toString()), Map.of()));
        });
    }

    @Test
    public void startedServerIsConfiguredOnceNetBeansHasInitializedIt() throws Exception {
        String python = interpreterFile("first");
        List<DidChangeConfigurationParams> messages = new ArrayList<>();
        LanguageServerDescription description = description();
        PythonLspConfiguration.serverStarted(project(python), description);
        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
        assertTrue("Nothing is sent before the server is initialized", messages.isEmpty());

        LanguageServerProviderAccessor.getINSTANCE().setBindings(description, bindings(messages));
        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals(1, messages.size());
        assertEquals(python, jedi(messages.get(0)).get("environment"));

        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals("Startup configuration is sent once", 1, messages.size());
    }

    @Test
    public void settingsAndInterpreterChangesKeepEachProjectsInterpreter() throws Exception {
        String firstPython = interpreterFile("first");
        String secondPython = interpreterFile("second");
        String updatedPython = interpreterFile("updated first");
        Project firstProject = project(firstPython);
        Project secondProject = project(secondPython);
        List<DidChangeConfigurationParams> firstMessages = new ArrayList<>();
        List<DidChangeConfigurationParams> secondMessages = new ArrayList<>();
        start(firstProject, firstMessages);
        start(secondProject, secondMessages);

        writeInterpreter(firstProject, updatedPython);
        PythonLspConfiguration.configureProject(firstProject);
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals(updatedPython, jedi(last(firstMessages)).get("environment"));
        assertEquals("Another project's server is not touched", 1, secondMessages.size());

        PythonLspConfiguration.configureRunningServers(Map.of("plugins",
                Map.of("jedi_completion", Map.of("enabled", false))));
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals(updatedPython, jedi(last(firstMessages)).get("environment"));
        assertEquals(secondPython, jedi(last(secondMessages)).get("environment"));
        assertEquals(Map.of("enabled", false), plugins(last(firstMessages)).get("jedi_completion"));

        List<DidChangeConfigurationParams> restartMessages = new ArrayList<>();
        start(firstProject, restartMessages);
        assertEquals("A restarted server gets the interpreter again",
                updatedPython, jedi(restartMessages.get(0)).get("environment"));
    }

    @Test
    public void serverThatHasExitedDoesNotStopTheOthersFromBeingConfigured() throws Exception {
        String python = interpreterFile("alive");
        List<DidChangeConfigurationParams> dead = new ArrayList<>();
        List<DidChangeConfigurationParams> alive = new ArrayList<>();
        start(project(python), dead);
        start(project(python), alive);
        dead.clear();
        alive.clear();
        failing.add(dead);
        try {
            PythonLspConfiguration.configureRunningServers(Map.of());
            PythonLspConfiguration.waitForPendingUpdates();
            PythonLspConfiguration.configureRunningServers(Map.of());
            PythonLspConfiguration.waitForPendingUpdates();
        } finally {
            failing.removeIf(list -> list == dead);
        }
        assertEquals(2, alive.size());
        assertEquals("The exited server is forgotten after its first failure", 1, dead.size());
    }

    @Test
    public void serverThatExitsAfterInitializationIsForgotten() throws Exception {
        Process process = process();
        LanguageServerDescription description = LanguageServerDescription.create(
                process.getInputStream(), process.getOutputStream(), process);
        Project project = project(interpreterFile("exits later"));
        List<DidChangeConfigurationParams> messages = new ArrayList<>();
        PythonLspConfiguration.serverStarted(project, description);
        LanguageServerProviderAccessor.getINSTANCE().setBindings(description, bindings(messages));
        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals(1, messages.size());
        process.destroy();
        PythonLspConfiguration.configureProject(project);
        PythonLspConfiguration.waitForPendingUpdates();
        assertEquals("An exited server gets no more settings", 1, messages.size());
    }

    @Test
    public void damagedPlatformListFallsBackToTheServersOwnEnvironment() throws Exception {
        Path platforms = PythonUtility.PLATFORMS.toPath();
        byte[] previous = Files.exists(platforms) ? Files.readAllBytes(platforms) : null;
        try {
            Files.createDirectories(platforms.getParent());
            Files.writeString(platforms, "[{\"cmd\": ");
            assertNull(PythonLspConfiguration.interpreter(project(""), Map.of()));
        } finally {
            if (previous == null) {
                Files.deleteIfExists(platforms);
            } else {
                Files.write(platforms, previous);
            }
        }
    }

    @Test
    public void serverThatExitsBeforeInitializationIsForgotten() throws Exception {
        Process process = process();
        LanguageServerDescription description = LanguageServerDescription.create(
                process.getInputStream(), process.getOutputStream(), process);
        PythonLspConfiguration.serverStarted(project(interpreterFile("exited")), description);
        process.destroy();
        List<DidChangeConfigurationParams> messages = new ArrayList<>();
        LanguageServerProviderAccessor.getINSTANCE().setBindings(description, bindings(messages));
        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
        assertTrue("A failed server must not receive startup configuration", messages.isEmpty());
    }

    private static Process process() {
        CompletableFuture<Process> exited = new CompletableFuture<>();
        return new Process() {
            @Override public InputStream getInputStream() { return InputStream.nullInputStream(); }
            @Override public InputStream getErrorStream() { return InputStream.nullInputStream(); }
            @Override public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
            @Override public int waitFor() { return 0; }
            @Override public int exitValue() { return 0; }
            @Override public boolean isAlive() { return !exited.isDone(); }
            @Override public void destroy() { exited.complete(this); }
            @Override public CompletableFuture<Process> onExit() { return exited; }
        };
    }

    private void start(Project project, List<DidChangeConfigurationParams> messages) throws Exception {
        LanguageServerDescription description = description();
        PythonLspConfiguration.serverStarted(project, description);
        LanguageServerProviderAccessor.getINSTANCE().setBindings(description, bindings(messages));
        PythonLspConfiguration.configurePendingServers();
        PythonLspConfiguration.waitForPendingUpdates();
    }

    private interface Check {
        void run() throws Exception;
    }

    private static void withDefaultPlatform(String python, Check check) throws Exception {
        Path platforms = PythonUtility.PLATFORMS.toPath();
        byte[] previous = Files.exists(platforms) ? Files.readAllBytes(platforms) : null;
        try {
            Files.createDirectories(platforms.getParent());
            Files.writeString(platforms, "[{\"cmd\":\"" + python.replace("\\", "\\\\") + "\",\"state\":\"true\"}]");
            check.run();
        } finally {
            if (previous == null) {
                Files.deleteIfExists(platforms);
            } else {
                Files.write(platforms, previous);
            }
        }
    }

    private String interpreterFile(String name) throws Exception {
        File file = new File(temporaryFolder.newFolder(name, "bin"), "python");
        assertTrue(file.createNewFile());
        return file.getPath();
    }

    private static Project project(String interpreter) throws Exception {
        FileObject directory = FileUtil.createMemoryFileSystem().getRoot().createFolder("project");
        Project project = new Project() {
            @Override public FileObject getProjectDirectory() { return directory; }
            @Override public Lookup getLookup() { return Lookup.EMPTY; }
        };
        writeInterpreter(project, interpreter);
        return project;
    }

    private static void writeInterpreter(Project project, String interpreter) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("nbproject.python_path", interpreter);
        try (OutputStream output = FileUtil.createData(project.getProjectDirectory(),
                "nbproject/project.properties").getOutputStream()) {
            properties.store(output, null);
        }
    }

    private static LanguageServerDescription description() {
        return LanguageServerDescription.create(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), null);
    }

    /** LSPBindings has a private constructor: (server, result, process) on NetBeans 29, plus a LanguageIdResolver on 31. */
    private static LSPBindings bindings(List<DidChangeConfigurationParams> messages) throws Exception {
        WorkspaceService workspace = new WorkspaceService() {
            @Override public void didChangeConfiguration(DidChangeConfigurationParams params) {
                messages.add(params);
                if (failing.stream().anyMatch(list -> list == messages)) {
                    // As lsp4j does when the server's stream is closed.
                    throw new IllegalStateException("server exited");
                }
            }
            @Override public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) { }
        };
        LanguageServer server = (LanguageServer) Proxy.newProxyInstance(LanguageServer.class.getClassLoader(),
                new Class<?>[]{LanguageServer.class}, (proxy, method, args) ->
                        "getWorkspaceService".equals(method.getName()) ? workspace : null);
        for (Constructor<?> constructor : LSPBindings.class.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (types.length >= 3 && types[0] == LanguageServer.class && types[1] == InitializeResult.class
                    && types[2] == Process.class) {
                Object[] arguments = new Object[types.length];
                arguments[0] = server;
                arguments[1] = new InitializeResult(new ServerCapabilities());
                constructor.setAccessible(true);
                return (LSPBindings) constructor.newInstance(arguments);
            }
        }
        throw new AssertionError(Arrays.toString(LSPBindings.class.getDeclaredConstructors()));
    }

    private static DidChangeConfigurationParams last(List<DidChangeConfigurationParams> messages) {
        return messages.get(messages.size() - 1);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> settings(DidChangeConfigurationParams params) {
        return (Map<String, Object>) ((Map<?, ?>) params.getSettings()).get("pylsp");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> plugins(DidChangeConfigurationParams params) {
        return (Map<String, Object>) settings(params).get("plugins");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> jedi(DidChangeConfigurationParams params) {
        return (Map<String, Object>) plugins(params).get("jedi");
    }
}
