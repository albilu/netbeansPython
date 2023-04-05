package org.netbeans.modules.python.tasklist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonDiagCollector {

    static String[] UNNECESSITY_CODES = new String[]{
        "F401",
        "F504",
        "F522",
        "F523",
        "F841"
    };
    public static List<ErrorDescription> errors;
    public static Pattern DIAG_REG = Pattern.compile("(.*):(\\d*):(\\d*): (\\w*) (.*)");

    private static String getOutput(FileObject fo) {
        try {
            if (fo == null) {
                return "";
            }
            return PythonUtility.getCommandOutput(new String[]{PythonUtility.getLspPythonExe(),
                "-m",
                "flake8",
                Paths.get(fo.getPath()).toString()}, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return "";
    }

    public static void parse(Source source, boolean projectOrNot) {
        format(getOutput(source.getFileObject()), source, projectOrNot);
    }

    private static void format(String diagOutput, Source source, boolean projectOrNot) {
        errors = new ArrayList<>();
        diagOutput.lines().forEach(diag -> {
            Matcher matcher = DIAG_REG.matcher(diag);
            if (matcher.find()) {
                ErrorDescription createErrorDescription = ErrorDescriptionFactory
                        .createErrorDescription(
                                getSeverity(matcher.group(4)),
                                matcher.group(5),
                                source.getDocument(projectOrNot),
                                Integer.parseInt(matcher.group(2))
                        );
                errors.add(createErrorDescription);

            }
        });
    }

    public static Severity getSeverity(String code) {
        Severity severity = Severity.WARNING;

        if (code.equals("E999") || (code.startsWith("F")
                && !StringUtils.equalsAny(code, UNNECESSITY_CODES))) {
            severity = Severity.ERROR;
        }
        return severity;
    }

    public static boolean hasErrors(FileObject fo) {
        try {
            if (!fo.isFolder() && !fo.getMIMEType().equals(PythonUtility.PYTHON_MIME_TYPE)) {
                return false;
            }
            ProcessBuilder p = new ProcessBuilder(PythonUtility.getLspPythonExe(),
                    "-m",
                    "pyflakes",
                    Paths.get(fo.getPath()).toString()
            );
            return !IOUtils.toString(p.start().getErrorStream(), StandardCharsets.UTF_8)
                    .strip().isEmpty();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;

    }
}
