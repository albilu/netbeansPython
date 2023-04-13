package org.netbeans.modules.python.coverage;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.text.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.netbeans.api.editor.document.LineDocument;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.gsf.codecoverage.api.CoverageManager;
import org.netbeans.modules.gsf.codecoverage.api.CoverageProvider;
import org.netbeans.modules.gsf.codecoverage.api.CoverageType;
import org.netbeans.modules.gsf.codecoverage.api.FileCoverageDetails;
import org.netbeans.modules.gsf.codecoverage.api.FileCoverageSummary;
import org.netbeans.modules.python.PythonOutputLine;
import org.netbeans.modules.python.PythonProject;
import org.netbeans.modules.python.PythonUtility;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonCodeCoverageProvider implements CoverageProvider {

    public static final Logger LOG = Logger.getLogger(PythonCodeCoverageProvider.class.getName());

    boolean enabled;
    boolean aggreg;
    PythonProject project;
    FileObject projectDir;

    public PythonCodeCoverageProvider(PythonProject project) {
        this.project = project;
        this.projectDir = project.getProjectDirectory();
    }

    @Override
    public boolean supportsHitCounts() {
        return true;
    }

    @Override
    public boolean supportsAggregation() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAggregating() {
        return aggreg;
    }

    @Override
    public void setAggregating(boolean bln) {
        this.aggreg = bln;
    }

    @Override
    public void setEnabled(boolean bln) {
        this.enabled = bln;
    }

    @Override
    public Set<String> getMimeTypes() {
        return new HashSet<>(Arrays.asList(PythonUtility.PYTHON_MIME_TYPE));
    }

    @Override
    public void clear() {
        try {
            String[] cmd = {
                PythonUtility.getProjectPythonExe(projectDir),
                "-m",
                "coverage",
                "erase"
            };
            coverageExecutor(cmd, "Clear Coverage Results");
            FileObject fileObject = projectDir.getFileObject(".coverage.json");
            if (fileObject != null) {
                fileObject.delete();
            }
            refresh();

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public FileCoverageDetails getDetails(FileObject fo, Document dcmnt) {
        JSONObject report = getReport();
        if (report == null) {
            return null;
        }
        String orElse = report.keySet().stream().filter((t)
                -> Paths.get(fo.getPath()).toString().endsWith(t)).findFirst()
                .orElse(null);

        if (orElse == null) {
            return null;
        }

        JSONObject jsonObject = report.getJSONObject(orElse);
        int lineCount = jsonObject.getJSONObject("summary").getInt("num_statements");//num_statements
        int executedLineCount = jsonObject.getJSONObject("summary").getInt("covered_lines");//covered_lines

        JSONArray executedLines = jsonObject.getJSONArray("executed_lines");//executed_lines
        JSONArray missingLines = jsonObject.getJSONArray("missing_lines");//missing_lines
        JSONArray executedBranches = jsonObject.getJSONArray("executed_branches");//executed_branches
        JSONArray missingBranches = jsonObject.getJSONArray("missing_branches");//missing_branches

        FileCoverageSummary fileCoverageSummary = new FileCoverageSummary(
                projectDir.getFileObject(orElse),
                orElse,
                lineCount,
                executedLineCount,
                0,
                0
        );

        return new PythonFileCoverageDetails(fo, dcmnt, fileCoverageSummary, executedLines,
                missingLines,
                executedBranches,
                missingBranches
        );
    }

    @Override
    public List<FileCoverageSummary> getResults() {
        List<FileCoverageSummary> coverageList = new ArrayList<>();
        JSONObject report = getReport();
        if (report == null) {
            return null;
        }
        report.keySet().stream().forEach((t) -> {
            JSONObject jsonObject = report.getJSONObject(t);
            int lineCount = jsonObject.getJSONObject("summary").getInt("num_statements");//num_statements
            int executedLineCount = jsonObject.getJSONObject("summary").getInt("covered_lines");//covered_lines
            coverageList.add(new FileCoverageSummary(
                    projectDir.getFileObject(t),
                    t,
                    lineCount,
                    executedLineCount,
                    0,
                    0
            ));

        });

        return coverageList;
    }

    @Override
    public String getTestAllAction() {
        return null;
    }

    private void coverageExecutor(String[] cmd, String message) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(cmd);

        LOG.info(() -> Arrays.toString(cmd));
        pb.directory(FileUtil.toFile(projectDir));
        PythonUtility.manageRunEnvs(pb);

        ExecutionDescriptor executorDescriptor = PythonUtility
                .getExecutorDescriptor(new PythonOutputLine(), null, () -> {
                }, false, false);

        ExecutionService service = ExecutionService
                .newService(() -> pb.start(), executorDescriptor, message);

        try {
            service.run().get();
        } catch (InterruptedException | ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void refresh() {
        CoverageManager.INSTANCE.resultsUpdated(project, PythonCodeCoverageProvider.this);
    }

    void collectReport() {
        if (this.isEnabled() && projectDir.getFileObject(".coverage") != null) {
            try {
                String[] cmd = {
                    PythonUtility.getProjectPythonExe(projectDir),
                    "-m",
                    "coverage",
                    "json",
                    //"--pretty-print",
                    "--ignore-errors"
                };
                coverageExecutor(cmd, "Collect Coverage");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private JSONObject getReport() {
        collectReport();
        FileObject fileObject = projectDir.getFileObject(".coverage.json");
        if (fileObject != null) {
            try {
                JSONObject jsonObject = new JSONObject(fileObject.asText());
                return jsonObject.getJSONObject("files");
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    class PythonFileCoverageDetails implements FileCoverageDetails {

        FileObject fo;
        Document doc;
        FileCoverageSummary summary;
        JSONArray executedLines;
        JSONArray missingLines;
        JSONArray executedBranches;
        JSONArray missingBranches;

        public PythonFileCoverageDetails(FileObject fo, Document doc,
                FileCoverageSummary summary, JSONArray executedLines,
                JSONArray missingLines,
                JSONArray executedBranches,
                JSONArray missingBranches) {
            this.fo = fo;
            this.doc = doc;
            this.summary = summary;
            this.executedLines = executedLines;
            this.missingLines = missingLines;
            this.executedBranches = executedBranches;
            this.missingBranches = missingBranches;
        }

        @Override
        public FileObject getFile() {
            return fo;
        }

        @Override
        public int getLineCount() {
            return LineDocumentUtils.getLineCount((LineDocument) doc);
        }

        @Override
        public boolean hasHitCounts() {
            return false;
        }

        @Override
        public long lastUpdated() {
            return projectDir.getFileObject(".coverage.json").lastModified().getTime();
        }

        @Override
        public FileCoverageSummary getSummary() {
            return summary;
        }

        @Override
        public CoverageType getType(int i) {

            int line = i + 1;

            boolean isExecutedL = executedLines.toList().stream().flatMap((t) -> Stream.of(t)).anyMatch(y -> y.equals(line));
            boolean isMissingL = missingLines.toList().stream().flatMap((t) -> Stream.of(t)).anyMatch(y -> y.equals(line));
            //Actually branches are Array of Array not sure yet for lines coverage objects
            boolean isMissingB = missingBranches.toList().stream().flatMap((t) -> Stream.of((List) t)).anyMatch(y -> y.contains(line));
            //boolean isExecutedB = executedBranches.toList().toList().stream().flatMap((t) -> Stream.of((List) t)).anyMatch(y -> y.contains(line));

            if (isMissingL) {
                return CoverageType.NOT_COVERED;
            } else if (isExecutedL && !isMissingB) {
                return CoverageType.COVERED;
            } else if (isExecutedL && isMissingB) {
                return CoverageType.PARTIAL;

            }
            return CoverageType.UNKNOWN;
        }

        @Override
        public int getHitCount(int i) {
            return summary.getExecutedLineCount();
        }

    }

}
