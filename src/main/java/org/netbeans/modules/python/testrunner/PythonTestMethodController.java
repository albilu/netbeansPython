package org.netbeans.modules.python.testrunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodController.TestMethod;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.spi.project.SingleMethod;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author albilu
 */
class PythonTestMethodController {

    static Pattern TEST_CLASS = Pattern.compile(".*\\nclass\\s+([^\\(|:]+).*:");
    static Pattern TEST_METHOD = Pattern.compile(".*def\\s+(test_[^\\(|:]+).*:");

    static void setMethods(Snapshot snapshot) {
        try {
            FileObject fo = snapshot.getSource().getFileObject();
            StyledDocument document = DataObject.find(fo).getLookup().lookup(EditorCookie.class).getDocument();
            List<TestMethod> computeMethods = computeMethods(document);
            if (computeMethods == null || computeMethods.isEmpty()) {
                return;
            }
            NbDocument.runAtomic(document, () -> {
                setTestMethodsImpl(document, computeMethods);
            });

        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    //Copy from org/netbeans/modules/gsf/testrunner/ui/api/TestMethodController.java
    private static void setTestMethodsImpl(StyledDocument doc, List<TestMethod> methods) {
        //FIXME-BUG Method Run annotation position change break action => contribution-welcome
        doc.putProperty(PythonTestMethodAnnotation.DOCUMENT_METHODS_KEY, methods);

        Map<TestMethod, PythonTestMethodAnnotation> annotations = (Map<TestMethod, PythonTestMethodAnnotation>) doc.getProperty(PythonTestMethodAnnotation.DOCUMENT_ANNOTATIONS_KEY);

        if (annotations == null) {
            annotations = new HashMap<>();
            doc.putProperty(PythonTestMethodAnnotation.DOCUMENT_ANNOTATIONS_KEY, annotations);
        }

        Map<Integer, TestMethod> annotationLines = (Map<Integer, TestMethod>) doc.getProperty(PythonTestMethodAnnotation.DOCUMENT_ANNOTATION_LINES_KEY);

        if (annotationLines == null) {
            annotationLines = new HashMap<>();
            doc.putProperty(PythonTestMethodAnnotation.DOCUMENT_ANNOTATION_LINES_KEY, annotationLines);
        }

        Map<TestMethod, PythonTestMethodAnnotation> removed = new HashMap<>(annotations);

        methods.forEach(tm -> removed.remove(tm));

        Set<TestMethod> added = new HashSet<>(methods);

        added.removeAll(annotations.keySet());

        for (TestMethod method : added) {
            PythonTestMethodAnnotation a = new PythonTestMethodAnnotation(method);
            NbDocument.addAnnotation(doc, method.preferred(), 0, a);
            annotations.put(method, a);
            int line = NbDocument.findLineNumber(doc, method.preferred().getOffset());
            annotationLines.put(line, method);
        }
        for (Map.Entry<TestMethod, PythonTestMethodAnnotation> e : removed.entrySet()) {
            NbDocument.removeAnnotation(doc, e.getValue());
            annotations.remove(e.getKey());
            int line = NbDocument.findLineNumber(doc, e.getKey().preferred().getOffset());
            annotationLines.remove(line);
        }
    }

    private static List<TestMethod> computeMethods(StyledDocument doc) {
        if (doc == null) {
            return null;
        }
        FileObject fo = NbEditorUtilities.getFileObject(doc);
        if (!isUnderTestSources(fo)) {
            return null;
        }
        List<TestMethod> methods = new ArrayList<>();
        try {
            String currentFile = doc.getText(0, doc.getEndPosition().getOffset());
            Stream<MatchResult> classResults = TEST_CLASS.matcher(currentFile).results();

            if (classResults.count() == 0) {
                return null;
            }

            TEST_METHOD.matcher(currentFile).results().forEach((methodMatch) -> {
                try {
                    int startOffset = methodMatch.start(1);
                    MatchResult classGroup = TEST_CLASS.matcher(currentFile).results()
                            .filter((classMatch) -> classMatch.end(1) < startOffset)
                            .reduce((first, second) -> second)
                            .orElse(null);
                    if (classGroup != null) {
                        String className = classGroup.group(1);
                        String testMethodName = methodMatch.group(1);
                        Position start = NbDocument.createPosition(doc, startOffset, Position.Bias.Forward);
                        Position end = NbDocument.createPosition(doc, methodMatch.end(1), Position.Bias.Backward);
                        methods.add(new TestMethod(className, new SingleMethod(fo, testMethodName), start, start, end));
                    }
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }

            });
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return methods;
    }

    private static boolean isUnderTestSources(FileObject fo) {
        Project owner = FileOwnerQuery.getOwner(fo);
        if (owner == null) {
            return false;
        }

        for (SourceGroup dir : ProjectUtils.getSources(owner).getSourceGroups("testsources")) {
            if (FileUtil.isParentOf(dir.getRootFolder(), fo)) {
                return true;
            }
        }
        return false;
    }
}
