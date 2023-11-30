package org.netbeans.modules.python.indexing;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.ErrorsCache;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.netbeans.modules.parsing.spi.indexing.support.IndexDocument;
import org.netbeans.modules.parsing.spi.indexing.support.IndexingSupport;
import org.netbeans.modules.python.PythonUtility;
import org.netbeans.modules.python.tasklist.PythonDiagCollector;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
public class PythonCustomIndexer extends CustomIndexer {

    public static final Logger LOG = Logger.getLogger(PythonCustomIndexer.class.getName());
    static Pattern PYTHON_CLASS = Pattern.compile(".*\\nclass\\s+([^\\(|:]+).*:");
    static Pattern PYTHON_METHOD = Pattern.compile(".*def\\s+([^\\(|:]+).*:");
    public static final String TYPE_FIELD = "classes";
    public static final String SYMBOLS_FIELD = "methods";

    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        long startTime = System.currentTimeMillis();
        int cnt = 0;
        if (context.isCancelled()) {
            LOG.fine("Indexer cancelled");
            return;
        }
        FileObject root = context.getRoot();
        Project owner = root != null ? FileOwnerQuery.getOwner(root) : null;
        boolean pyProject = owner != null && owner.getClass()
                .getName().equals("org.netbeans.modules.python.project.PythonProject");
        boolean pyLibPath = root != null && (root.getName().endsWith("Lib") || (root.getParent() != null
                && root.getParent().getName().equals("lib")));
        if (pyProject || pyLibPath) {
            try {
                IndexingSupport is = IndexingSupport.getInstance(context);
                for (Indexable file : files) {
                    File toFile = Paths.get(file.getURL().toURI()).toFile();
                    FileObject fo = FileUtil.toFileObject(toFile);
                    if (fo.getExt().equals("py")) {
                        boolean notSkipped = !StringUtils.containsAny(toFile.toPath().toString(),
                                PythonUtility.EXCLUDED_DIRS);
                        if (pyProject && notSkipped) {
                            String content = getContent(fo);
                            setErrors(fo, context, file);
                            setClasses(content, is, file);
                            setMethods(content, is, file);
                            cnt++;
                        }
                        if (pyLibPath && notSkipped) {
                            String content = getContent(fo);
                            setClasses(content, is, file);
                            //Not relevant to index python methods
                            //setMethods(content, is, file);
                            cnt++;

                        }
                    }
                }
            } catch (BadLocationException | URISyntaxException | IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        long endTime = System.currentTimeMillis();
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Processed {0} files for {1} in {2}ms.", new Object[]{
                cnt, cnt > 0 ? root.getPath() : "null", endTime - startTime});
        }
    }

    private String getContent(FileObject fo) throws BadLocationException, IOException {
        EditorCookie lookup = fo.getLookup().lookup(EditorCookie.class);
        StyledDocument openDocument = lookup.openDocument();
        return openDocument.getText(0, openDocument.getEndPosition().getOffset());
    }

    private void setClasses(String content, IndexingSupport is, Indexable file) throws IOException, BadLocationException {

        IndexDocument createDocument = is.createDocument(file);
        PYTHON_CLASS.matcher(content).results()
                .forEach((match) -> {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(match.group(1));
                    stringBuilder.append("#");
                    stringBuilder.append(match.start(1));
                    createDocument.addPair(TYPE_FIELD, stringBuilder.toString(), true, true);
                });

        is.addDocument(createDocument);
    }

    private void setErrors(FileObject fo, Context context, Indexable file) {
        List errors = new ArrayList<>();
        boolean hasErrors = PythonDiagCollector.hasErrors(fo);
        PythonDefaultError pythonDefaultError = new PythonDefaultError(
                "PARSING", "badging_error", "badging_error", fo, 0, 0, org.netbeans.modules.csl.api.Severity.ERROR);

        errors.add(pythonDefaultError);
        ErrorsCache
                .setErrors(context.getRootURI(), file,
                        hasErrors ? errors : Collections.emptyList(),
                        new PythonErrorConvertor());
    }

    private void setMethods(String content, IndexingSupport is, Indexable file) throws IOException, BadLocationException {
        IndexDocument createDocument = is.createDocument(file);
        PYTHON_METHOD.matcher(content).results()
                .forEach((methodMatch) -> {
                    String className = " ";
                    int startOffset = methodMatch.start(1);
                    MatchResult classGroup = PYTHON_CLASS.matcher(content).results()
                            .filter((classMatch) -> classMatch.end(1) < startOffset)
                            .reduce((first, second) -> second)
                            .orElse(null);
                    if (classGroup != null) {
                        className = classGroup.group(1);
                    }

                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(methodMatch.group(1));
                    stringBuilder.append("#");
                    stringBuilder.append(methodMatch.start(1));
                    stringBuilder.append("#");
                    stringBuilder.append(className);
                    createDocument.addPair(SYMBOLS_FIELD, stringBuilder.toString(), true, true);
                });

        is.addDocument(createDocument);
    }
}
