/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.eval.langid;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import opennlp.tools.langdetect.LanguageDetectorModel;
import opennlp.tools.util.normalizer.CharSequenceNormalizer;
import opennlp.tools.util.normalizer.EmojiCharSequenceNormalizer;
import opennlp.tools.util.normalizer.NumberCharSequenceNormalizer;
import opennlp.tools.util.normalizer.ShrinkCharSequenceNormalizer;
import opennlp.tools.util.normalizer.TwitterCharSequenceNormalizer;
import org.apache.tika.language.detect.LanguageDetector;


public class LanguageIDWrapper {

    static LanguageDetectorModel LANG_MODEL;

    static int MAX_TEXT_LENGTH = 50000;

    public static void loadBuiltInModels() throws IOException {
        LANG_MODEL = new LanguageDetectorModel(
                LanguageIDWrapper.class.getResource("/opennlp/model5.bin"));
    }

    public static void loadModels(Path path) throws IOException {
        LANG_MODEL = new LanguageDetectorModel(path.toFile());
    }

    private static CharSequenceNormalizer[] getNormalizers() {
        return new CharSequenceNormalizer[]{
                EmojiCharSequenceNormalizer.getInstance(),
                TikaUrlCharSequenceNormalizer.getInstance(),
                TwitterCharSequenceNormalizer.getInstance(),
                NumberCharSequenceNormalizer.getInstance(),
                ShrinkCharSequenceNormalizer.getInstance()
        };
    }

    private final opennlp.tools.langdetect.LanguageDetector detector;
    public LanguageIDWrapper() {
        detector = new ProbingLanguageDetector(LANG_MODEL);
    }

    public List<Language> getProbabilities(String s) {
        opennlp.tools.langdetect.Language[] detected = detector.predictLanguages(s);
        List<Language> ret = new ArrayList<>();
        for (int i = 0; i < detected.length; i++) {
            ret.add(new Language(detected[i].getLang(), detected[i].getConfidence()));
        }
        return ret;
    }

    public static void setMaxTextLength(int maxTextLength) {
        MAX_TEXT_LENGTH = maxTextLength;
    }

    private static class TikaUrlCharSequenceNormalizer implements CharSequenceNormalizer {
        //use this custom copy/paste of opennlo to avoid long, long hang with mail_regex
        //TIKA-2777
        private static final Pattern URL_REGEX = Pattern.compile("https?://[-_.?&~;+=/#0-9A-Za-z]{10,10000}");
        private static final Pattern MAIL_REGEX = Pattern.compile("[-_.0-9A-Za-z]{1,100}@[-_0-9A-Za-z]{1,100}[-_.0-9A-Za-z]{1,100}");
        private static final TikaUrlCharSequenceNormalizer INSTANCE = new TikaUrlCharSequenceNormalizer();

        public static TikaUrlCharSequenceNormalizer getInstance() {
            return INSTANCE;
        }

        private TikaUrlCharSequenceNormalizer() {
        }

        @Override
        public CharSequence normalize(CharSequence charSequence) {
            String modified = URL_REGEX.matcher(charSequence).replaceAll(" ");
            return MAIL_REGEX.matcher(modified).replaceAll(" ");
        }
    }
}
