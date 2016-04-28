package eu.kanade.tachiyomi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;

public final class ChapterRecognition {

    private static final Pattern cleanWithToken = Pattern.compile("ch[^0-9]?\\s*(\\d+[\\.,]?\\d+)($|\\b)");
    private static final Pattern uncleanWithToken = Pattern.compile("ch[^0-9]?\\s*(\\d+[\\.,]?\\d*)");
    private static final Pattern withAlphaPostfix = Pattern.compile("(\\d+[\\.,]?\\d*\\s*)([a-z])($|\\b)");
    private static final Pattern cleanNumber = Pattern.compile("(\\d+[\\.,]?\\d+)($|\\b)");
    private static final Pattern uncleanNumber = Pattern.compile("(\\d+[\\.,]?\\d*)");
    private static final Pattern withColon = Pattern.compile("(\\d+[\\.,]?\\d*\\s*:)([^\\d]|$)");
    private static final Pattern startingNumber = Pattern.compile("^(\\d+[\\.,]?\\d*)");

    private static final Pattern pUnwanted =
            Pattern.compile("(\\b|\\d)(v|ver|vol|version|volume)\\.?\\s*\\d+\\b");
    private static final Pattern pPart =
            Pattern.compile("(\\b|\\d)part\\s*\\d+.+");

    private ChapterRecognition() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    public static void parseChapterNumber(Chapter chapter, Manga manga) {
        if (chapter.chapter_number != -1)
            return;

        String name = chapter.name.toLowerCase();
        Matcher matcher;

        // Safest option, the chapter has a token prepended and nothing at the end of the number
        matcher = cleanWithToken.matcher(name);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1));
            return;
        }

        // a number with a single alpha prefix is parsed as sub-chapter
        matcher = withAlphaPostfix.matcher(name);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1)) + parseAlphaPostFix(matcher.group(2));
            return;
        }

        // the chapter has a token prepended and something at the end of the number
        matcher = uncleanWithToken.matcher(name);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1));
            return;
        }

        // Remove anything related to the volume or version
        name = pUnwanted.matcher(name).replaceAll("$1");

        List<Float> occurrences;

        // If there's only one number, use it
        matcher = uncleanNumber.matcher(name);
        occurrences = getAllOccurrences(matcher);
        if (occurrences.size() == 1) {
            chapter.chapter_number =  occurrences.get(0);
            return;
        }

        // If it has a colon, the chapter number should be that one
        matcher = withColon.matcher(name);
        occurrences = getAllOccurrences(matcher);
        if (occurrences.size() == 1) {
            chapter.chapter_number =  occurrences.get(0);
            return;
        }

        // Prefer numbers without anything appended
        matcher = cleanNumber.matcher(name);
        occurrences = getAllOccurrences(matcher);
        if (occurrences.size() == 1) {
            chapter.chapter_number =  occurrences.get(0);
            return;
        }

        // This can lead to issues if two numbers are separated by an space
        name = name.replaceAll("\\s+", "");

        // Try to remove the manga name from the chapter, and try again
        String mangaName = replaceIrrelevantCharacters(manga.title);
        String nameWithoutManga = difference(mangaName, name).trim();
        if (!nameWithoutManga.isEmpty()) {
            matcher = uncleanNumber.matcher(nameWithoutManga);
            occurrences = getAllOccurrences(matcher);
            if (occurrences.size() == 1) {
                chapter.chapter_number =  occurrences.get(0);
                return;
            }
        }

        // TODO more checks (maybe levenshtein?)

        // try splitting the name in parts an pick the first valid one
        String[] nameParts = chapter.name.split("-");
        Chapter dummyChapter = Chapter.create();
        if (nameParts.length > 1) {
            for (String part : nameParts) {
                dummyChapter.name = part;
                parseChapterNumber(dummyChapter, manga);
                if (dummyChapter.chapter_number >= 0) {
                    chapter.chapter_number = dummyChapter.chapter_number;
                    return;
                }
            }
        }

        // Strip anything after "part xxx" and try that
        matcher = pPart.matcher(name);
        if (matcher.find()) {
            name = pPart.matcher(name).replaceAll("$1");
            dummyChapter.name = name;
            parseChapterNumber(dummyChapter, manga);
            if (dummyChapter.chapter_number >= 0) {
                chapter.chapter_number = dummyChapter.chapter_number;
                return;
            }
        }


        // check for a number either at the start or right after the manga title
        matcher = startingNumber.matcher(name);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1));
            return;
        }
        matcher = startingNumber.matcher(nameWithoutManga);
        if (matcher.find()) {
            chapter.chapter_number = Float.parseFloat(matcher.group(1));
            return;
        }
    }

    /**
     * x.a -> x.1, x.b -> x.2, etc
     */
    private static float parseAlphaPostFix(String postfix) {
        char alpha = postfix.charAt(0);
        return Float.parseFloat("0." + Integer.toString((int)alpha - 96));
    }

    public static List<Float> getAllOccurrences(Matcher matcher) {
        List<Float> occurences = new ArrayList<>();
        while (matcher.find()) {
            // Match again to get only numbers from the captured text
            String text = matcher.group();
            Matcher m = uncleanNumber.matcher(text);
            if (m.find()) {
                try {
                    Float value = Float.parseFloat(m.group(1).replaceAll(",", "."));
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
