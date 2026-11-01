package org.netbeans.modules.python.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;

/**
 * The changes a formatter's {@link TextEdit}s make to a text, optionally
 * limited to the selected lines.
 * <p>
 * Formatters describe the same change in different ways: pylsp's YAPF plugin
 * deletes lines and inserts their formatted versions with separate edits,
 * autopep8 replaces the whole text. The edits are therefore applied to a copy
 * of the text, and the result is compared with the original. Each change
 * found that way is applied or rejected as a whole, so a deletion is never
 * applied without its insertion.
 */
final class FormatterEdits {

    /** Replaces {@code [start, end)} of the original text with {@code text}. */
    record Change(int start, int end, String text) {
    }

    private record Segment(Change change, boolean reindents) {
    }

    private FormatterEdits() {
    }

    /**
     * The text after the edits, applied as LSP specifies: every range refers
     * to the original text, and inserts at the same position keep their
     * order. Returns {@code null} if an edit is invalid or edits overlap.
     */
    static String apply(String text, List<? extends TextEdit> edits) {
        int[] lineStarts = lineStarts(text);
        List<Change> changes = new ArrayList<>();
        for (TextEdit edit : edits) {
            if (edit == null || edit.getRange() == null) {
                return null;
            }
            int start = offset(text, lineStarts, edit.getRange().getStart());
            int end = offset(text, lineStarts, edit.getRange().getEnd());
            if (start < 0 || end < start) {
                return null;
            }
            changes.add(new Change(start, end, edit.getNewText() == null ? "" : edit.getNewText()));
        }
        // A stable sort: inserts at the same position keep their order and
        // come before a replacement starting there.
        changes.sort(Comparator.comparingInt(Change::start).thenComparingInt(Change::end));
        StringBuilder result = new StringBuilder();
        int copied = 0;
        for (Change change : changes) {
            if (change.start() < copied) {
                return null;
            }
            result.append(text, copied, change.start()).append(change.text());
            copied = change.end();
        }
        return result.append(text, copied, text.length()).toString();
    }

    /**
     * The changes from {@code original} to {@code formatted}, in ascending
     * order, that lie within {@code [startLimit, endLimit]} ({@code null}:
     * no limit).
     * <p>
     * Changed lines are split where the code (the text without whitespace)
     * before the split is the same in both versions, so the parts can be
     * applied independently. A part reaching outside the limits is rejected.
     * Python requires consistent indentation, so if a rejected part
     * re-indents code, no part re-indents code.
     */
    static List<Change> changes(String original, String formatted, Integer startLimit, Integer endLimit) {
        List<String> before = lines(original);
        List<String> after = lines(formatted);
        int first = 0;
        while (first < before.size() && first < after.size() && before.get(first).equals(after.get(first))) {
            first++;
        }
        int endBefore = before.size();
        int endAfter = after.size();
        while (endBefore > first && endAfter > first
                && before.get(endBefore - 1).equals(after.get(endAfter - 1))) {
            endBefore--;
            endAfter--;
        }
        List<String> removed = before.subList(first, endBefore);
        List<String> added = after.subList(first, endAfter);
        int start = String.join("", before.subList(0, first)).length();

        List<Segment> segments = new ArrayList<>();
        List<int[]> boundaries = boundaries(removed, added);
        for (int k = 1; k < boundaries.size(); k++) {
            int[] from = boundaries.get(k - 1);
            int[] to = boundaries.get(k);
            String oldText = String.join("", removed.subList(from[0], to[0]));
            String newText = String.join("", added.subList(from[1], to[1]));
            if (!oldText.equals(newText)) {
                int prefix = commonPrefix(oldText, newText);
                int suffix = commonSuffix(oldText, newText, prefix);
                Change change = new Change(start + prefix, start + oldText.length() - suffix,
                        newText.substring(prefix, newText.length() - suffix));
                segments.add(new Segment(change, !indents(oldText).equals(indents(newText))));
            }
            start += oldText.length();
        }

        boolean reindentsOutside = segments.stream()
                .anyMatch(segment -> segment.reindents() && !within(segment.change(), startLimit, endLimit));
        List<Change> changes = new ArrayList<>();
        for (Segment segment : segments) {
            if (within(segment.change(), startLimit, endLimit) && !(segment.reindents() && reindentsOutside)) {
                changes.add(segment.change());
            }
        }
        return changes;
    }

    /**
     * Where {@code offset} of the original text is after the changes: before
     * the same non-whitespace character, or, if it was followed by
     * whitespace, after the same preceding one.
     */
    static int map(String original, List<Change> changes, int offset) {
        boolean beforeCode = offset < original.length() && !Character.isWhitespace(original.charAt(offset));
        int shift = 0;
        for (Change change : changes) {
            boolean insertion = change.start() == change.end();
            if (offset < change.start() || (insertion && offset == change.start() && !beforeCode)) {
                break;
            }
            if (offset >= change.end()) {
                shift += change.text().length() - (change.end() - change.start());
                continue;
            }
            // Inside the replaced text: find the same code in the new text.
            String text = change.text();
            int code = code(original.substring(change.start(), offset)).length();
            int index = 0;
            while (index < text.length() && (code > 0 || (beforeCode && Character.isWhitespace(text.charAt(index))))) {
                if (!Character.isWhitespace(text.charAt(index))) {
                    code--;
                }
                index++;
            }
            return change.start() + shift + index;
        }
        return offset + shift;
    }

    /**
     * Line boundaries {i, j} where {@code removed[0..i)} and
     * {@code added[0..j)} contain the same code, from {0, 0} to
     * {removed.size(), added.size()}.
     */
    private static List<int[]> boundaries(List<String> removed, List<String> added) {
        List<String> a = new ArrayList<>();
        removed.forEach(line -> a.add(code(line)));
        List<String> b = new ArrayList<>();
        added.forEach(line -> b.add(code(line)));
        List<int[]> boundaries = samePrefix(a, b);
        // Then the same from the end, after the last boundary found so far.
        List<int[]> fromEnd = samePrefix(reversed(a), reversed(b));
        int[] last = boundaries.get(boundaries.size() - 1);
        for (int k = fromEnd.size() - 1; k >= 0; k--) {
            int i = a.size() - fromEnd.get(k)[0];
            int j = b.size() - fromEnd.get(k)[1];
            if (i >= last[0] && j >= last[1] && (i > last[0] || j > last[1])) {
                last = new int[]{i, j};
                boundaries.add(last);
            }
        }
        return boundaries;
    }

    /** Boundaries {i, j} where {@code a[0..i)} and {@code b[0..j)} join to the same string. */
    private static List<int[]> samePrefix(List<String> a, List<String> b) {
        int common = commonPrefix(String.join("", a), String.join("", b));
        List<int[]> boundaries = new ArrayList<>();
        int i = 0;
        int j = 0;
        int lengthA = 0;
        int lengthB = 0;
        while (true) {
            if (lengthA == lengthB) {
                if (lengthA > common) {
                    break;
                }
                boundaries.add(new int[]{i, j});
                // Pair blank lines with blank lines.
                boolean blankA = i < a.size() && a.get(i).isEmpty();
                boolean blankB = j < b.size() && b.get(j).isEmpty();
                boolean nextA = i < a.size() && (blankA || !blankB);
                boolean nextB = j < b.size() && (blankB || !blankA);
                if (!nextA && !nextB) {
                    break;
                }
                if (nextA) {
                    lengthA += a.get(i++).length();
                }
                if (nextB) {
                    lengthB += b.get(j++).length();
                }
            } else if (lengthA < lengthB) {
                if (i == a.size()) {
                    break;
                }
                lengthA += a.get(i++).length();
            } else {
                if (j == b.size()) {
                    break;
                }
                lengthB += b.get(j++).length();
            }
        }
        return boundaries;
    }

    private static List<String> reversed(List<String> lines) {
        List<String> reversed = new ArrayList<>();
        for (int k = lines.size() - 1; k >= 0; k--) {
            reversed.add(new StringBuilder(lines.get(k)).reverse().toString());
        }
        return reversed;
    }

    private static boolean within(Change change, Integer startLimit, Integer endLimit) {
        return (startLimit == null || change.start() >= startLimit)
                && (endLimit == null || change.end() <= endLimit);
    }

    /** The text without whitespace. */
    private static String code(String text) {
        StringBuilder code = new StringBuilder();
        for (int k = 0; k < text.length(); k++) {
            if (!Character.isWhitespace(text.charAt(k))) {
                code.append(text.charAt(k));
            }
        }
        return code.toString();
    }

    /** The leading whitespace of each line that is not blank. */
    private static List<String> indents(String text) {
        List<String> indents = new ArrayList<>();
        for (String line : lines(text)) {
            int length = 0;
            while (length < line.length() && line.charAt(length) != '\n'
                    && Character.isWhitespace(line.charAt(length))) {
                length++;
            }
            if (length < line.length() && line.charAt(length) != '\n') {
                indents.add(line.substring(0, length));
            }
        }
        return indents;
    }

    /** The lines of the text, each with its line break. */
    private static List<String> lines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start) + 1;
            if (end == 0) {
                end = text.length();
            }
            lines.add(text.substring(start, end));
            start = end;
        }
        return lines;
    }

    private static int[] lineStarts(String text) {
        List<String> lines = lines(text);
        int[] starts = new int[lines.size() + (text.endsWith("\n") || text.isEmpty() ? 1 : 0)];
        for (int k = 1; k < starts.length; k++) {
            starts[k] = starts[k - 1] + lines.get(k - 1).length();
        }
        return starts;
    }

    private static int offset(String text, int[] lineStarts, Position position) {
        if (position == null || position.getLine() < 0 || position.getCharacter() < 0) {
            return -1;
        }
        // Python formatters use the line after the last one as the end of the
        // text, also when it has no final newline. LSP positions beyond a
        // line or the text are clamped.
        if (position.getLine() >= lineStarts.length) {
            return text.length();
        }
        int start = lineStarts[position.getLine()];
        int end = position.getLine() + 1 < lineStarts.length
                ? lineStarts[position.getLine() + 1] - 1 : text.length();
        return start + Math.min(position.getCharacter(), end - start);
    }

    private static int commonPrefix(String original, String replacement) {
        int max = Math.min(original.length(), replacement.length());
        int length = 0;
        while (length < max && original.charAt(length) == replacement.charAt(length)) {
            length++;
        }
        // Never split a surrogate pair between unchanged and replaced text.
        return length > 0 && Character.isHighSurrogate(original.charAt(length - 1)) ? length - 1 : length;
    }

    private static int commonSuffix(String original, String replacement, int prefix) {
        int max = Math.min(original.length(), replacement.length()) - prefix;
        int length = 0;
        while (length < max && original.charAt(original.length() - 1 - length)
                == replacement.charAt(replacement.length() - 1 - length)) {
            length++;
        }
        return length > 0 && Character.isLowSurrogate(original.charAt(original.length() - length)) ? length - 1 : length;
    }
}
