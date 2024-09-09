package org.netbeans.modules.python.repl;

import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.UIUtil;
import com.pty4j.PtyProcess;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.project.PythonProject;

/**
 *
 * @author albilu
 */
public class PythonTerminalConnector {

    public static TtyConnector createTtyConnector(String type,
            PythonProject project) {
        try {
            Map<String, String> envs = PythonUtility.getEnvs();
            String[] command;

            if (!UIUtil.isWindows) {
                envs.put("TERM", "xterm-256color");
            }

            if (type.equals("ptpython")) {
                command = new String[]{/*osShell[0], osShell[1],*/
                    PythonUtility.getLspPythonExe(), "-m", "ptpython"};
            } else if (type.equals("ipython")) {
                command = new String[]{/*osShell[0], osShell[1],*/
                    //                    "ipython"
                    PythonUtility.getLspPythonExe(), "-m", "IPython"
                };
            } else if (type.equals(PythonProject.POETRY)) {
                command = new String[]{/*osShell[0], osShell[1],*/
                    //                    "ipython"
                    PythonUtility.getProjectPythonExe(project.getProjectDirectory()), "-m", "poetry",
                    "shell"
                };

            } else {
                String[] osShell = PythonUtility.getOsShell();
                if (UIUtil.isWindows) {
                    command = new String[]{osShell[0]};
                } else if (UIUtil.isMac) {
                    command = new String[]{osShell[0], "--login"};
                } else {
                    command = new String[]{osShell[0]};

                }
                com.pty4j.PtyProcessBuilder processBuilder = new com.pty4j.PtyProcessBuilder()
                        .setCommand(command)
                        .setEnvironment(envs)
                        .setConsole(false)
                        .setWindowsAnsiColorEnabled(true)
                        .setDirectory(Paths.get(project
                                .getProjectDirectory().getPath()).toString());

                PtyProcess process = /*PtyProcess.exec(command, envs, project
                        .getProjectDirectory().getPath())*/ processBuilder.start();
                PtyProcessTtyConnector loggingPtyProcessTtyConnector
                        = new PtyProcessTtyConnector(process,
                                Charset.forName("UTF-8"));
                loggingPtyProcessTtyConnector.write(UIUtil.isWindows
                        ? Paths.get(project.getProjectDirectory().getPath()).resolve(".venv\\Scripts\\activate.bat").toString()
                        : "source " + Paths.get(project.getProjectDirectory().getPath()).resolve(".venv/bin/activate").toString());
                loggingPtyProcessTtyConnector.write(new byte[]{process
                    .getEnterKeyCode()});

                return loggingPtyProcessTtyConnector;
            }

            com.pty4j.PtyProcessBuilder processBuilder = new com.pty4j.PtyProcessBuilder();

            processBuilder.setCommand(command);
            processBuilder.setEnvironment(envs);
            processBuilder.setConsole(false);
            processBuilder.setWindowsAnsiColorEnabled(true);
            if (project != null) {
                processBuilder.setDirectory(Paths.get(project
                        .getProjectDirectory().getPath()).toString());
            }

            PtyProcess process = /*PtyProcess.exec(command, envs, null)*/ processBuilder.start();

            return new PtyProcessTtyConnector(process,
                    Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
