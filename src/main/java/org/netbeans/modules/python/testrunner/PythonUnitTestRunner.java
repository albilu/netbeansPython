package org.netbeans.modules.python.testrunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.print.ConvertedLine;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.netbeans.api.extexecution.print.LineConvertors;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.gsf.testrunner.api.Report;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.api.RerunType;
import org.netbeans.modules.gsf.testrunner.api.Status;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.ui.api.Manager;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.project.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.coverage.PythonCodeCoverageProvider;
import org.netbeans.spi.project.SingleMethod;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author albilu
 */
public class PythonUnitTestRunner implements PythonTestRunner {

    public static final Logger LOG = Logger.getLogger(PythonUnitTestRunner.class.getName());
    static File testRunner = PythonUtility.TEST_RUNNER;

    @Override
    public void runSingleTest(PythonProject project, DataObject dob) {

        FileObject pfo = project.getProjectDirectory();
        FileObject fo = dob.getPrimaryFile();

        Object[] runParams = PythonTestManager.getTestRunParams(project);
        String runner = runParams[0].toString();

        if (runner.equals("pytest")) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("PyTest not supported at the moment", NotifyDescriptor.INFORMATION_MESSAGE));
            return;
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(pfo));
        PythonUtility.manageRunEnvs(pb);

        List<String> argList = new ArrayList<>();
        try {
            String projectPythonExe = PythonUtility.getProjectPythonExe(fo);

            argList.add(projectPythonExe);

            getCoverageArgs(argList, project);

            argList.add(testRunner.toPath().toString());

            argList.add("-f");
            argList.add(Paths.get(fo.getPath()).toString());

            pb.command(argList);
            LOG.info(() -> Arrays.toString(argList.toArray()));

            runner(project, PythonTestManager.getTestManager(),
                    PythonTestManager.getSession(project, runner), pb, projectPythonExe, testRunner);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void getCoverageArgs(List<String> argList, PythonProject project) {
        String name = "tests";
        if (project != null) {
            SourceGroup[] sourceGroups = ProjectUtils.getSources(project).getSourceGroups("testsources");
            for (SourceGroup sourceGroup : sourceGroups) {
                name = sourceGroup.getRootFolder().getName();
            }
        }
        argList.add("-m");
        argList.add("coverage");
        argList.add("run");
        argList.add("--branch");
        PythonCodeCoverageProvider cp = getCoverageProvider(project);
        if (cp.isAggregating()) {
            argList.add("--append");
        }
        argList.add("--source=.");
        argList.add(String.format("--omit=%s/**/*.py", name));

    }

    @Override
    public void runAllTests(PythonProject project, DataObject dob) {

        FileObject pfo = project.getProjectDirectory();
        FileObject fo = dob.getPrimaryFile();

        Object[] runParams = PythonTestManager.getTestRunParams(project);
        String runner = runParams[0].toString();

        if (runner.equals("pytest")) {
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("PyTest not supported at the moment", NotifyDescriptor.INFORMATION_MESSAGE));
            return;
        }

        String[] params = (String[]) runParams[1];

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(FileUtil.toFile(pfo));
        PythonUtility.manageRunEnvs(pb);

        List<String> argList = new ArrayList<>();
        try {
            String projectPythonExe = PythonUtility.getProjectPythonExe(fo);

            argList.add(projectPythonExe);

            getCoverageArgs(argList, project);

            argList.add(testRunner.toPath().toString());

            argList.add("-d");
            argList.add(Paths.get(fo.getPath()).toString());
            argList.add("-p");
            argList.add(params.length == 1 ? params[0] : ArrayUtils.subarray(params, 1, params.length)[0]);

            pb.command(argList);

            LOG.info(() -> Arrays.toString(argList.toArray()));

            runner(project, PythonTestManager.getTestManager(), PythonTestManager.getSession(project, runner), pb, projectPythonExe, testRunner);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void runner(PythonProject project, Manager manager, TestSession testSession, ProcessBuilder pb,
            String projectPythonExe, File testRunner) {
        ExecutionDescriptor execDescriptor = new ExecutionDescriptor()
                .frontWindow(true)
                .frontWindowOnError(true)
                .showProgress(true)
                .outConvertorFactory(new PythonTestOutputLine(manager, testSession))
                .errConvertorFactory(new PythonOutputLine())
                .postExecution(() -> {
                    manager.sessionFinished(testSession);
                    PythonCodeCoverageProvider cp = getCoverageProvider(project);
                    if (cp.isEnabled()) {
                        cp.refresh();
                    }
                });

        ExecutionService service = ExecutionService
                .newService(() -> pb.start(), execDescriptor,
                        "Test Runner");

        service.run();

        testSession.setRerunHandler(new RerunHandler() {
            @Override
            public void rerun() {
                service.run();
            }

            @Override
            public void rerun(Set<Testcase> set) {
                //Doctest replay not supported
                List<String> argList = new ArrayList<>();
                List<String> builder = new ArrayList<>();
                for (Testcase testcase : set) {
                    String location = testcase.getLocation();
                    String caseName = testcase.getName();
                    String displayName = testcase.getDisplayName();
                    if ((caseName != null && !caseName.startsWith("subtest")) && !displayName.endsWith(caseName)) {
                        displayName = String.format("%s.%s", displayName, caseName);
                    }
                    if (location != null) {
                        argList.clear();
                        String file = location.split("##")[0];
                        String[] split = displayName.split(" ")[0].split("\\.");
                        String[] methodName = ArrayUtils.subarray(split, split.length - 2, split.length);
                        argList.add(projectPythonExe);
                        //FIXME-BUG need escape the --omit regex in this case => contribution-welcome
                        //getCoverageArgs(argList, project);
                        argList.add(testRunner.toPath().toString());
                        argList.add("-m");
                        argList.add(String.join(".", methodName));
                        argList.add("-f");
                        argList.add(file);
                        builder.add(String.join(" ", argList));

                    }

                }
                String[] osShell = PythonUtility.getOsShell();
                ArrayList<String> arrayList = new ArrayList<>(new LinkedHashSet<>(builder));

                String[] cmd = {osShell[0], osShell[1], String.join((Utilities.isUnix() || Utilities.isMac()) ? " && "
                    : "&", arrayList)};

                pb.command(cmd);

                LOG.info(() -> Arrays.toString(cmd));

                ExecutionService service = ExecutionService
                        .newService(() -> pb.start(), execDescriptor,
                                "Test Runner");
                service.run();

            }

            @Override
            public boolean enabled(RerunType rt) {
                return true;
            }

            @Override
            public void addChangeListener(ChangeListener cl) {
            }

            @Override
            public void removeChangeListener(ChangeListener cl) {
            }
        });
    }

    private PythonCodeCoverageProvider getCoverageProvider(PythonProject project) {
        PythonCodeCoverageProvider cp = project.getLookup().lookup(PythonCodeCoverageProvider.class);
        return cp;
    }

    @Override
    public void runTestMethod(PythonProject project, TestMethodController.TestMethod lookup) {
        try {
            SingleMethod method = lookup.method();
            String testClassName = lookup.getTestClassName();
            FileObject file = method.getFile();

            String projectPythonExe = PythonUtility.getProjectPythonExe(file);

            Object[] runParams = PythonTestManager.getTestRunParams(project);
            String runner = runParams[0].toString();

            if (runner.equals("pytest")) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("PyTest not supported at the moment", NotifyDescriptor.INFORMATION_MESSAGE));
                return;
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(FileUtil.toFile(project.getProjectDirectory())/*.getParentFile()*/);
            PythonUtility.manageRunEnvs(pb);

            String methodName = method.getMethodName();

            List<String> argList = new ArrayList<>();
            argList.add(projectPythonExe);
            getCoverageArgs(argList, project);
            argList.add(testRunner.toPath().toString());
            argList.add("-m");
            argList.add(String.format("%s.%s", testClassName, methodName));
            argList.add("-f");
            argList.add(Paths.get(file.getPath()).toString());

            pb.command(argList);

            LOG.info(() -> Arrays.toString(argList.toArray()));

            runner(project, PythonTestManager.getTestManager(), PythonTestManager.getSession(project, runner), pb, projectPythonExe, testRunner);

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    static class PythonTestOutputLine implements ExecutionDescriptor.LineConvertorFactory {

        Manager manager;
        TestSession testSession;
        TestSuite testSuite;
        static Pattern PATTERN = Pattern.compile("\\%\\s+(time=([0-9.]+))\\s+(testname=([^\\s]+)|subtest=[^\\s]+|[^\\s]+)\\s+\\(([^\\)]+)\\)\\s+(message=|reason=|\\(([^\\)]+)\\))?((test_path=(.*))|(.*)test_path=(.*))|\\%\\s+(time=([0-9.]+))\\s+(testname=([^\\s]+)|subtest=[^\\s]+|[^\\s]+)\\s+(message=|reason=|\\(([^\\)]+)\\))?((test_path=(.*))|(.*)test_path=(.*))");

        public PythonTestOutputLine(Manager manager, TestSession testSession) {
            this.testSession = testSession;
            this.manager = manager;
        }

        LineConvertor LINE_CONVERTOR = (String line) -> {
            boolean suiteStarted = StringUtils.startsWith(line, "%SUITE_STARTING%");
//                    boolean testStarted = StringUtils.startsWith(line, "%TEST_STARTED%");
            boolean testFailed = StringUtils.startsWith(line, "%TEST_FAILED%");
            boolean testSkipped = StringUtils.startsWith(line, "%TEST_SKIPPED%");
            boolean testError = StringUtils.startsWith(line, "%TEST_ERROR%");
//                    boolean suiteFailures = StringUtils.startsWith(line, "%SUITE_FAILURES%");
//                    boolean suiteErrors = StringUtils.startsWith(line, "%SUITE_ERRORS%");
//                    boolean suiteSuccess = StringUtils.startsWith(line, "%SUITE_SUCCESS%");
            boolean testFinished = StringUtils.startsWith(line, "%TEST_FINISHED%");
            boolean suiteFinished = StringUtils.startsWith(line, "%SUITE_FINISHED%");

            if (suiteStarted) {
                testSuite = new TestSuite(line.split(" ")[1]);
                testSession.addSuite(testSuite);
//                        manager.testStarted(testSession);
                manager.displaySuiteRunning(testSession, testSuite);
            }
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.find()) {
                long time = (long) Float.parseFloat(matcher.group(14) != null ? matcher.group(14) : matcher.group(2)) * 1000;
                String methodName = matcher.group(15) != null ? matcher.group(15) : (matcher.group(4) != null ? matcher.group(4) : matcher.group(3));
                String subTest = matcher.group(7);
                String testCaseName = subTest != null ? matcher.group(5) + " " + subTest : matcher.group(5);
                testCaseName = (testCaseName == null || testCaseName.isEmpty()
                        || NumberUtils.isDigits(testCaseName)) ? methodName : testCaseName;
                String message = matcher.group(11) != null ? matcher.group(11) : matcher.group(8);
                String location = matcher.group(10) != null ? matcher.group(10) : matcher.group(12);

                if (testFailed) {
                    List<String> list = new ArrayList<>();
                    PythonTestCase pythonTestcase = new PythonTestCase(methodName,
                            testCaseName, "", testSession);
                    testSuite.addTestcase(pythonTestcase);
                    pythonTestcase.setStatus(Status.FAILED);
                    pythonTestcase.setTimeMillis(time);
                    list.add(message);
                    pythonTestcase.addOutputLines(list);
                    pythonTestcase.setLocation(location);
                    manager.displayOutput(testSession, String.join(" ", line.split(" ", 2)[1]), true);

                }
                if (testFinished) {
                    PythonTestCase pythonTestcase = new PythonTestCase(methodName,
                            testCaseName, "", testSession);
                    testSuite.addTestcase(pythonTestcase);
                    pythonTestcase.setStatus(Status.PASSED);
                    pythonTestcase.setTimeMillis(time);
                    pythonTestcase.setLocation(location);

                }
                if (testSkipped) {
                    List<String> list = new ArrayList<>();
                    PythonTestCase pythonTestcase = new PythonTestCase(methodName,
                            testCaseName, "", testSession);
                    testSuite.addTestcase(pythonTestcase);
                    pythonTestcase.setStatus(Status.SKIPPED);
                    pythonTestcase.setTimeMillis(time);
                    list.add(message);
                    pythonTestcase.addOutputLines(list);
                    pythonTestcase.setLocation(location);

                }
                if (testError) {
                    List<String> list = new ArrayList<>();
                    PythonTestCase pythonTestcase = new PythonTestCase(methodName,
                            testCaseName, "", testSession);
                    testSuite.addTestcase(pythonTestcase);
                    pythonTestcase.setStatus(Status.ERROR);
                    pythonTestcase.setTimeMillis(time);
                    pythonTestcase.setLocation(location);
                    list.add(message);
                    pythonTestcase.addOutputLines(list);
                    manager.displayOutput(testSession, String.join(" ", line.split(" ", 2)[1]), true);
                }
            }
            if (suiteFinished) {
                Report report = testSession.getReport(testSession.getSessionResult().getElapsedTime());
                manager.displayReport(testSession, report, true); // `true` means don't display "running..." next to the suite
                testSession.finishSuite(testSuite);

            }

            PythonUtility.noModuleInstalledHandler(line);
            return Collections.singletonList(
                    ConvertedLine.forText(line, null));
        };

        @Override
        public LineConvertor newLineConvertor() {
            return LineConvertors
                    .proxy(LINE_CONVERTOR, PythonUtility.FILE_CONVERTOR, PythonUtility.HTTP_CONVERTOR);

        }

    }

}
