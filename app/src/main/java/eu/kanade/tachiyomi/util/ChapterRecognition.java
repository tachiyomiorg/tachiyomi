package eu.kanade.tachiyomi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;

public class ChapterRecognition {

    private static final Pattern p1 = Pattern.compile("ch[^0-9]?\\s*(\\d+[\\.,]?\\d*)");
    private static final Pattern p2 = Pattern.compile("(\\d+[\\.,]?\\d*)");
    private static final Pattern p3 = Pattern.compile("(\\d+[\\.,]?\\d*\\s*:)");

    private static final Pattern pUnwanted =
            Pattern.compile("\\b(v|ver|vol|version|volume)\\.?\\s*\\d+\\b");

    public static void parseChapterNumber(Chapter chapter, Manga manga) {
        if (chapter.chapter_number != -1)
            return;

        String name = chapter.name.toLowerCase();
        Matcher matcher;

        // Safest option, the chapter has a token prepended
        matcher = p1.matcher(name);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1));
            return;
        }

        // Remove anything related to the volume or version
        name = pUnwanted.matcher(name).replaceAll("");

        List<Float> occurrences;

        // If there's only one number, use it
        matcher = p2.matcher(name);
        occurrences = getAllOccurrences(matcher);
        if (occurrences.size() == 1) {
            chapter.chapter_number =  occurrences.get(0);
            return;
        }

        // If it has a colon, the chapter number should be that one
        matcher = p3.matcher(name);
        occurrences = getAllOccurrences(matcher);
        if (occurrences.size() == 1) {
            chapter.chapter_number =  occurrences.get(0);
            return;
        }

        // This can lead to issues if two numbers are separated by an space
        name = name.replaceAll("\\s+", "");

        // Try to remove the manga name from the chapter, and try again
        String mangaName = replaceIrrelevantCharacters(manga.title);
        String nameWithoutManga = difference(mangaName, name);
        if (!nameWithoutManga.isEmpty()) {
            matcher = p2.matcher(nameWithoutManga);
            occurrences = getAllOccurrences(matcher);
            if (occurrences.size() == 1) {
                chapter.chapter_number =  occurrences.get(0);
                return;
            }
        }

        // TODO more checks (maybe levenshtein?)

    }

    public static List<Float> getAllOccurrences(Matcher matcher) {
        List<Float> occurences = new ArrayList<>();
        while (matcher.find()) {
            // Match again to get only numbers from the captured text
            String text = matcher.group();
            Matcher m = p2.matcher(text);
            if (m.find()) {
                try {
                    Float value = Float.parseFloat(m.group(1));
                    if (!occurences.contains(value)) {
                        occurences.add(value);
                    }
                } catch (NumberFormatException e) { /* Do nothing */ }
            }
        }
        return occurences;
    }

    public static String replaceIrrelevantCharacters(String str) {
        return str.replaceAll("\\s+", "").toLowerCase();
    }

    public static String difference(String str1, String str2) {
        if (str1 == null) {
            return str2;
        }
        if (str2 == null) {
            return str1;
        }
        int at = indexOfDifference(str1, str2);
        if (at == -1) {
            return "";
        }
        return str2.substring(at);
    }
    public static int indexOfDifference(String str1, String str2) {
        if (str1 == str2) {
            return -1;
        }
        if (str1 == null || str2 == null) {
            return 0;
        }
        int i;
        for (i = 0; i < str1.length() && i < str2.length(); ++i) {
            if (str1.charAt(i) != str2.charAt(i)) {
                break;
            }
        }
        if (i < str2.length() || i < str1.length()) {
            return i;
        }
        return -1;
    }
}
