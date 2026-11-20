package org.netbeans.modules.python;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

/**
 * Issue #144 with a real pylsp. Run with
 * -Dpython.lsp.test.executable=/absolute/path/to/python-with-pylsp.
 */
public class PythonLspInterpreterIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void sharedServerCompletesLibraryInstalledOnlyInSelectedProjectEnvironment() throws Exception {
        String serverPython = System.getProperty("python.lsp.test.executable");
        Assume.assumeTrue("Set python.lsp.test.executable to run the real pylsp integration test",
                serverPython != null && !serverPython.isBlank());
        Path root = temporaryFolder.newFolder("project with spaces").toPath();
        Path environment = root.resolve("project environment");
        run(serverPython, "-m", "venv", "--without-pip", environment.toString());
        String projectPython = environment.resolve(System.getProperty("os.name").startsWith("Windows")
                ? "Scripts/python.exe" : "bin/python").toString();
        Path packages = Path.of(run(projectPython, "-c", "import sysconfig; print(sysconfig.get_path('purelib'))").strip());
        Files.createDirectories(packages);
        Files.writeString(packages.resolve("nb_project_only_library.py"), "project_only_symbol = 42\n");
        assertEquals("None", run(projectPython, "-c", "import importlib.util; print(importlib.util.find_spec('pylsp'))").strip());
        assertEquals("None", run(serverPython, "-c", "import importlib.util; print(importlib.util.find_spec('nb_project_only_library'))").strip());

        Process process = new ProcessBuilder(serverPython, "-m", "pylsp")
                .directory(root.toFile()).redirectError(root.resolve("pylsp.log").toFile()).start();
        Future<Void> listening = null;
        LanguageServer server = null;
        try {
            LanguageClient client = (LanguageClient) Proxy.newProxyInstance(LanguageClient.class.getClassLoader(),
                    new Class<?>[]{LanguageClient.class}, (proxy, method, args) ->
                            CompletableFuture.class.isAssignableFrom(method.getReturnType())
                                    ? CompletableFuture.completedFuture(null) : null);
            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client,
                    process.getInputStream(), process.getOutputStream());
            listening = launcher.startListening();
            server = launcher.getRemoteProxy();
            InitializeParams initialize = new InitializeParams();
            initialize.setRootUri(root.toUri().toString());
            initialize.setCapabilities(new ClientCapabilities());
            server.initialize(initialize).get(30, TimeUnit.SECONDS);
            server.initialized(new InitializedParams());
            String source = "import nb_project_only_library\nnb_project_only_library.project_";
            String uri = root.resolve("lesson.py").toUri().toString();
            server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
                    new TextDocumentItem(uri, "python", 1, source)));
            CompletionParams completion = new CompletionParams(new TextDocumentIdentifier(uri),
                    new Position(1, "nb_project_only_library.project_".length()));

            Map<String, Object> defaults = PythonLspConfiguration.readSettings();
            server.getWorkspaceService().didChangeConfiguration(PythonLspConfiguration.configuration(defaults, null));
            assertFalse("Without the interpreter the server reproduces issue #144",
                    labels(server, completion).contains("project_only_symbol"));
            server.getWorkspaceService().didChangeConfiguration(
                    PythonLspConfiguration.configuration(defaults, projectPython));
            assertTrue("Completion must find the selected interpreter's installed module",
                    labels(server, completion).contains("project_only_symbol"));

            server.getWorkspaceService().didChangeConfiguration(
                    PythonLspConfiguration.configuration(defaults, serverPython));
            assertFalse("Switching interpreters must stop using the previous environment",
                    labels(server, completion).contains("project_only_symbol"));
        } finally {
            if (server != null && process.isAlive()) {
                try {
                    server.shutdown().get(5, TimeUnit.SECONDS);
                    server.exit();
                } catch (Exception ignored) {
                    // The process is still terminated below if startup or a request failed.
                }
            }
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            if (listening != null) {
                listening.cancel(true);
            }
        }
    }

    private List<String> labels(LanguageServer server, CompletionParams params) throws Exception {
        var result = server.getTextDocumentService().completion(params).get(30, TimeUnit.SECONDS);
        List<CompletionItem> items = result.isLeft() ? result.getLeft() : result.getRight().getItems();
        List<String> labels = new ArrayList<>();
        for (CompletionItem item : items) {
            labels.add(item.getLabel());
        }
        return labels;
    }

    private String run(String... command) throws Exception {
        Path output = temporaryFolder.newFile().toPath();
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue("Python command timed out", process.waitFor(30, TimeUnit.SECONDS));
            String text = Files.readString(output, StandardCharsets.UTF_8);
            assertEquals(text, 0, process.exitValue());
            return text;
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }
}
