/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.sprd.contacts;

import android.content.ContentValues;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.FullNameStyle;
import android.text.TextUtils;
import android.util.ArraySet;

import java.lang.Character.UnicodeBlock;
import java.util.Locale;

/**
 * The purpose of this class is to split a full name into given names and last
 * name. The logic only supports having a single last name. If the full name has
 * multiple last names the output will be incorrect.
 * <p>
 * Core algorithm:
 * <ol>
 * <li>Remove the suffixes (III, Ph.D., M.D.).</li>
 * <li>Remove the prefixes (Mr., Pastor, Reverend, Sir).</li>
 * <li>Assign the last remaining token as the last name.</li>
 * <li>If the previous word to the last name is one from LASTNAME_PREFIXES, use
 * this word also as the last name.</li>
 * <li>Assign the rest of the words as the "given names".</li>
 * </ol>
 */
public class NameSplitter {

    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();

    // This includes simplified and traditional Chinese
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();

    private final Locale mLocale;
    private final String mLanguage;

    public static class Name {
        public String givenNames;
        public String familyName;
        public int fullNameStyle;

        public Name() {
        }

        public Name(String givenNames, String familyName) {
            this.givenNames = givenNames;
            this.familyName = familyName;
        }

        public void fromValues(ContentValues values) {
            givenNames = values.getAsString(StructuredName.GIVEN_NAME);
            familyName = values.getAsString(StructuredName.FAMILY_NAME);

            Integer integer = values.getAsInteger(StructuredName.FULL_NAME_STYLE);
            fullNameStyle = integer == null ? FullNameStyle.UNDEFINED : integer;
        }

        @Override
        public String toString() {
            return "[given: " + givenNames + " family: " + familyName + "]";
        }
    }

    public NameSplitter(Locale locale) {
        // TODO: refactor this to use <string-array> resources
        mLocale = locale != null ? locale : Locale.getDefault();
        mLanguage = mLocale.getLanguage().toLowerCase();
    }

    /**
     * Concatenates components of a name according to the rules dictated by the name style.
     *
     */
    public String join(Name name) {
        switch (name.fullNameStyle) {
            case FullNameStyle.CJK:
            case FullNameStyle.CHINESE:
            case FullNameStyle.KOREAN:
                return join(name.familyName, name.givenNames, false);

            case FullNameStyle.JAPANESE:
                return join(name.familyName, name.givenNames, true);

            default:
                return join(name.givenNames, name.familyName, true);

        }
    }

    /**
     * Concatenates parts of a full name inserting spaces and commas as specified.
     */
    private String join(String part1, String part2, boolean useSpace) {
        part1 = part1 == null ? null: part1.trim();
        part2 = part2 == null ? null: part2.trim();

        boolean hasPart1 = !TextUtils.isEmpty(part1);
        boolean hasPart2 = !TextUtils.isEmpty(part2);

        boolean isSingleWord = true;
        String singleWord = null;


        if (hasPart1) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part1;
            }
        }

        if (hasPart2) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part2;
            }
        }

        if (isSingleWord) {
            return singleWord;
        }

        StringBuilder sb = new StringBuilder();

        if (hasPart1) {
            sb.append(part1);
        }

        if (hasPart2) {
            if (hasPart1) {
                if (useSpace) {
                    sb.append(' ');
                }
            }
            sb.append(part2);
        }

        return sb.toString();
    }

    /**
     * If the supplied name style is undefined, returns a default based on the language,
     * otherwise returns the supplied name style itself.
     *
     * @param nameStyle See {@link FullNameStyle}.
     */
    public int getAdjustedFullNameStyle(int nameStyle) {
        if (nameStyle == FullNameStyle.UNDEFINED) {
            if (JAPANESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.JAPANESE;
            } else if (KOREAN_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.KOREAN;
            } else if (CHINESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.CHINESE;
            } else {
                return FullNameStyle.WESTERN;
            }
        } else if (nameStyle == FullNameStyle.CJK) {
            if (JAPANESE_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.JAPANESE;
            } else if (KOREAN_LANGUAGE.equals(mLanguage)) {
                return FullNameStyle.KOREAN;
            } else {
                return FullNameStyle.CHINESE;
            }
        }
        return nameStyle;
    }

    /**
     * Makes the best guess at the expected full name style based on the character set
     * used in the supplied name.  If the phonetic name is also supplied, tries to
     * differentiate between Chinese, Japanese and Korean based on the alphabet used
     * for the phonetic name.
     */
    public void guessNameStyle(Name name) {
        guessFullNameStyle(name);
    }

    /**
     * Makes the best guess at the expected full name style based on the character set
     * used in the supplied name.
     */
    private void guessFullNameStyle(NameSplitter.Name name) {
        if (name.fullNameStyle != FullNameStyle.UNDEFINED) {
            return;
        }

        int bestGuess = guessFullNameStyle(name.givenNames);
        // A mix of Hanzi and latin chars are common in China, so we have to go through all names
        // if the name is not JANPANESE or KOREAN.
        if (bestGuess != FullNameStyle.UNDEFINED && bestGuess != FullNameStyle.CJK
                && bestGuess != FullNameStyle.WESTERN) {
            name.fullNameStyle = bestGuess;
            return;
        }

        int guess = guessFullNameStyle(name.familyName);
        if (guess != FullNameStyle.UNDEFINED) {
            if (guess != FullNameStyle.CJK && guess != FullNameStyle.WESTERN) {
                name.fullNameStyle = guess;
                return;
            }
            bestGuess = guess;
        }

        name.fullNameStyle = bestGuess;
    }

    public int guessFullNameStyle(String name) {
        if (name == null) {
            return FullNameStyle.UNDEFINED;
        }

        int nameStyle = FullNameStyle.UNDEFINED;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);

                if (!isLatinUnicodeBlock(unicodeBlock)) {

                    if (isCJKUnicodeBlock(unicodeBlock)) {
                        // We don't know if this is Chinese, Japanese or Korean -
                        // trying to figure out by looking at other characters in the name
                        return guessCJKNameStyle(name, offset + Character.charCount(codePoint));
                    }

                    if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.JAPANESE;
                    }

                    if (isKoreanUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.KOREAN;
                    }
                }
                nameStyle = FullNameStyle.WESTERN;
            }
            offset += Character.charCount(codePoint);
        }
        return nameStyle;
    }

    private int guessCJKNameStyle(String name, int offset) {
        int length = name.length();
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.JAPANESE;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.KOREAN;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return FullNameStyle.CJK;
    }

    private static boolean isLatinUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.BASIC_LATIN ||
                unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(UnicodeBlock block) {
        return block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == UnicodeBlock.CJK_RADICALS_SUPPLEMENT
                || block == UnicodeBlock.CJK_COMPATIBILITY
                || block == UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES ||
                unicodeBlock == UnicodeBlock.HANGUL_JAMO ||
                unicodeBlock == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.KATAKANA ||
                unicodeBlock == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
                unicodeBlock == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                unicodeBlock == UnicodeBlock.HIRAGANA;
    }
}
