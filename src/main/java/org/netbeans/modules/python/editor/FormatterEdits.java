package org.netbeans.modules.python.editor;

import java.util.ArrayList;
import java.util.Arrays;
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

    /** At most this many token insertions and deletions are aligned in a change. */
    private static final int MAX_TOKEN_CHANGES = 2000;

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
     * Where {@code offset} of the original text is after the changes. Inside
     * a change, the code of both versions is split into tokens, and the
     * offset stays before the same token, or, if it was followed by
     * whitespace or by a token the formatter removed (such as optional
     * parentheses), after the last preceding token it kept.
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
            return change.start() + shift
                    + mapInside(original.substring(change.start(), change.end()), change.text(),
                            offset - change.start(), beforeCode);
        }
        return offset + shift;
    }

    /** Where {@code offset} of {@code oldText} is in {@code newText}. */
    private static int mapInside(String oldText, String newText, int offset, boolean beforeCode) {
        List<int[]> oldTokens = tokens(oldText);
        List<int[]> newTokens = tokens(newText);
        int[] match = matchTokens(oldText, oldTokens, newText, newTokens);
        if (beforeCode) {
            // Before or inside the same token, if the formatter kept it.
            for (int k = 0; k < oldTokens.size(); k++) {
                int[] token = oldTokens.get(k);
                if (token[1] > offset) {
                    if (match[k] >= 0) {
                        return newTokens.get(match[k])[0] + Math.max(0, offset - token[0]);
                    }
                    break;
                }
            }
        }
        // After the last kept token before the offset.
        for (int k = oldTokens.size() - 1; k >= 0; k--) {
            int[] token = oldTokens.get(k);
            if (token[1] <= offset && match[k] >= 0) {
                return newTokens.get(match[k])[1];
            }
        }
        return 0;
    }

    /**
     * Code tokens {start, end}: runs of letters, digits and underscores, and
     * single other characters that are not whitespace.
     */
    private static List<int[]> tokens(String text) {
        List<int[]> tokens = new ArrayList<>();
        int k = 0;
        while (k < text.length()) {
            int c = text.codePointAt(k);
            int end = k + Character.charCount(c);
            if (Character.isWhitespace(c)) {
                k = end;
                continue;
            }
            if (word(c)) {
                while (end < text.length() && word(text.codePointAt(end))) {
                    end += Character.charCount(text.codePointAt(end));
                }
            }
            tokens.add(new int[]{k, end});
            k = end;
        }
        return tokens;
    }

    private static boolean word(int c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }

    /**
     * For each old token, the index of the same new token in a longest
     * common subsequence of the tokens, or -1 if the formatter removed it.
     * Uses Myers' O(ND) algorithm; formatters change few tokens. Tokens
     * that are the same at the start and end are matched first, and if more
     * than {@link #MAX_TOKEN_CHANGES} tokens differ, only those are matched.
     */
    private static int[] matchTokens(String oldText, List<int[]> oldTokens, String newText, List<int[]> newTokens) {
        int n = oldTokens.size();
        int m = newTokens.size();
        String[] a = new String[n];
        for (int k = 0; k < n; k++) {
            a[k] = oldText.substring(oldTokens.get(k)[0], oldTokens.get(k)[1]);
        }
        String[] b = new String[m];
        for (int k = 0; k < m; k++) {
            b[k] = newText.substring(newTokens.get(k)[0], newTokens.get(k)[1]);
        }
        int[] match = new int[n];
        Arrays.fill(match, -1);
        int prefix = 0;
        while (prefix < n && prefix < m && a[prefix].equals(b[prefix])) {
            match[prefix] = prefix;
            prefix++;
        }
        while (n > prefix && m > prefix && a[n - 1].equals(b[m - 1])) {
            match[n - 1] = m - 1;
            n--;
            m--;
        }
        a = Arrays.copyOfRange(a, prefix, n);
        b = Arrays.copyOfRange(b, prefix, m);
        n -= prefix;
        m -= prefix;
        int max = n + m;
        int[] v = new int[2 * max + 2];
        List<int[]> trace = new ArrayList<>();
        int found = -1;
        for (int d = 0; d <= Math.min(max, MAX_TOKEN_CHANGES) && found < 0; d++) {
            for (int k = -d; k <= d; k += 2) {
                int x = k == -d || (k != d && v[max + k - 1] < v[max + k + 1])
                        ? v[max + k + 1] : v[max + k - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a[x].equals(b[y])) {
                    x++;
                    y++;
                }
                v[max + k] = x;
                if (x >= n && y >= m) {
                    found = d;
                    break;
                }
            }
            // Keep the diagonals -d..d of this step for the backtrack.
            trace.add(Arrays.copyOfRange(v, max - d, max + d + 1));
        }
        if (found < 0) {
            return match;
        }
        int x = n;
        int y = m;
        for (int d = found; d >= 0; d--) {
            int k = x - y;
            int previousK;
            int previousX;
            if (d == 0) {
                previousK = 0;
                previousX = 0;
            } else {
                int[] previous = trace.get(d - 1);
                boolean down = k == -d || (k != d && previous[k - 1 + d - 1] < previous[k + 1 + d - 1]);
                previousK = down ? k + 1 : k - 1;
                previousX = previous[previousK + d - 1];
            }
            int previousY = previousX - previousK;
            // The snake of this step: matching tokens up to (x, y).
            int startX = d == 0 ? 0 : (previousK == k + 1 ? previousX : previousX + 1);
            int startY = startX - k;
            while (x > startX && y > startY) {
                x--;
                y--;
                match[prefix + x] = prefix + y;
            }
            x = previousX;
            y = previousY;
        }
        return match;
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
