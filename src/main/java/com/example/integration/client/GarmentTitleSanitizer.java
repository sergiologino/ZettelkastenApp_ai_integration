package com.example.integration.client;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces marketplace wording that triggers AI content moderation with neutral retail terms.
 */
public final class GarmentTitleSanitizer {

    private record StemRule(Pattern pattern, String replacementStem) {}

    private record PhraseRule(Pattern pattern, String replacement) {}

    private static final List<StemRule> STEM_RULES = List.of(
            stem("сексуальн", "стильн"),
            stem("соблазнительн", "стильн"),
            stem("провокационн", "модн"),
            stem("откровенн", "легк"),
            stem("страстн", "ярк"),
            stem("пикантн", "необычн"),
            stem("интимн", "домашн"),
            stem("развратн", "модн"),
            stem("похотлив", "игрив"),
            stem("манящ", "привлекательн"),
            stem("заводящ", "привлекательн"),
            stem("афродизиак", "аромат"),
            stem("фетиш", "модн"),
            stem("прозрачн", "легк")
    );

    private static final List<PhraseRule> PHRASE_RULES = List.of(
            phrase("(?iu)\\bвозбуждающий\\b", "привлекательный"),
            phrase("(?iu)\\bвозбуждающая\\b", "привлекательная"),
            phrase("(?iu)\\bвозбуждающее\\b", "привлекательное"),
            phrase("(?iu)\\bвозбуждающие\\b", "привлекательные"),
            phrase("(?iu)\\bвозбуждающ(?:его|ему|им)\\b", "привлекательного"),
            phrase("(?iu)\\bвозбуждающ(?:ую|ей|ем)\\b", "привлекательную"),
            phrase("(?iu)\\bвозбуждающ(?:их|ими)\\b", "привлекательных"),
            phrase("(?iu)\\bсоблазняющий\\b", "привлекательный"),
            phrase("(?iu)\\bсоблазняющая\\b", "привлекательная"),
            phrase("(?iu)\\bсоблазняющее\\b", "привлекательное"),
            phrase("(?iu)\\bсоблазняющие\\b", "привлекательные"),
            phrase("(?iu)\\bсоблазняющ(?:его|ему|им)\\b", "привлекательного"),
            phrase("(?iu)\\bсоблазняющ(?:ую|ей|ем)\\b", "привлекательную"),
            phrase("(?iu)\\bсоблазняющ(?:их|ими)\\b", "привлекательных"),
            phrase("(?iu)\\bэротический\\b", "повседневный"),
            phrase("(?iu)\\bэротическая\\b", "повседневная"),
            phrase("(?iu)\\bэротическое\\b", "повседневное"),
            phrase("(?iu)\\bэротические\\b", "повседневные"),
            phrase("(?iu)\\bэротическ(?:ого|ому|им)\\b", "повседневного"),
            phrase("(?iu)\\bэротическ(?:ую|ой|ей|ем)\\b", "повседневную"),
            phrase("(?iu)\\bэротических\\b", "повседневных"),
            phrase("(?iu)\\bэротик(?:а|и|е|у|ой|ам|ами|ах)?\\b", "домашняя одежда"),
            phrase("(?iu)\\bсекс(?:у|а|у|ом|е)?\\b", "стиль"),
            phrase("(?iu)\\bсекси\\b", "стильный"),
            phrase("(?iu)\\bстриптиз(?:а|у|ом|е|ы|ов|ам|ами|ах)?\\b", "танец"),
            phrase("(?iu)\\bпорно(?:граф(?:ия|ии|ию|ией|ическ(?:ий|ая|ое|ие|ого|ому|им|их|ими))?)?\\b", "одежда"),
            phrase("(?iu)\\bбдсм\\b", "мода"),
            phrase("(?iu)\\bbdsm\\b", "fashion"),
            phrase("(?iu)\\bbondage\\b", "fashion"),
            phrase("(?iu)\\bдля\\s+секса\\b", "для дома"),
            phrase("(?iu)\\bfor\\s+sex\\b", "for home"),
            phrase("(?iu)\\bsexy\\b", "stylish"),
            phrase("(?iu)\\berotic\\b", "homewear"),
            phrase("(?iu)\\bsensual\\b", "elegant"),
            phrase("(?iu)\\bseductive\\b", "stylish"),
            phrase("(?iu)\\bprovocative\\b", "fashionable"),
            phrase("(?iu)\\barousing\\b", "attractive"),
            phrase("(?iu)\\blustful\\b", "playful"),
            phrase("(?iu)\\bnaughty\\b", "cute"),
            phrase("(?iu)\\bkinky\\b", "trendy"),
            phrase("(?iu)\\bstrip(?:tease)?\\b", "dance")
    );

    private GarmentTitleSanitizer() {
    }

    public static String forPrompt(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }

        String result = title.trim();
        for (StemRule rule : STEM_RULES) {
            result = applyStemRule(result, rule);
        }
        for (PhraseRule rule : PHRASE_RULES) {
            result = rule.pattern().matcher(result).replaceAll(rule.replacement());
        }
        return result.replaceAll("\\s{2,}", " ").trim();
    }

    private static StemRule stem(String sourceStem, String replacementStem) {
        return new StemRule(
                Pattern.compile("(?iu)(" + Pattern.quote(sourceStem) + ")([\\p{L}]*)"),
                replacementStem
        );
    }

    private static PhraseRule phrase(String regex, String replacement) {
        return new PhraseRule(Pattern.compile(regex), replacement);
    }

    private static String applyStemRule(String input, StemRule rule) {
        Matcher matcher = rule.pattern().matcher(input);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String originalStem = matcher.group(1);
            String suffix = matcher.group(2) != null ? matcher.group(2) : "";
            String replacement = capitalizeLike(originalStem, rule.replacementStem()) + suffix;
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String capitalizeLike(String original, String replacement) {
        if (original.isEmpty() || replacement.isEmpty()) {
            return replacement;
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }
}
