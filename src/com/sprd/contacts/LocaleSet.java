/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.annotation.NonNull;
import android.os.LocaleList;
import java.util.Locale;

public class LocaleSet {

    private final LocaleList mLocaleList;

    private LocaleSet(LocaleList localeList) {
        mLocaleList = localeList;
    }

    public static LocaleSet newDefault() {
        return new LocaleSet(LocaleList.getDefault());
    }

    /**
     * Returns the primary locale, which may not be the first item of {@link #getAllLocales}.
     * (See {@link LocaleList})
     */
    public @NonNull Locale getPrimaryLocale() {
        return Locale.getDefault();
    }
}
