/*
 * Copyright 2019 Steven Stewart-Gallus.
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
 * limitations under the License.
 */
package com.sstewartgallus.peacod.interop;

import com.sstewartgallus.peacod.runtime.ConstantBootstraps;
import com.sstewartgallus.peacod.indify.annotations.Bootstrap;
import com.sstewartgallus.peacod.indify.annotations.Indy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class ContextCalls {

    private ContextCalls() {
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:copy")
    public @interface Copy {

        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "SET:ELEMENT:memory")
    public @interface Set {
        @SuppressWarnings("unused")
        Class<?> T();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "GET:ELEMENT:memory")
    public @interface Get {
        @SuppressWarnings("unused")
        Class<?> T();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:mul")
    public @interface Mul {
        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:gather")
    public @interface Gather {
        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();

        @SuppressWarnings("unused")
        int Stride();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:scatter")
    public @interface Scatter {
        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();

        @SuppressWarnings("unused")
        int Stride();
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:sum")
    public @interface Sum {
        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "CALL:METHOD:dot")
    public @interface Dot {
        @SuppressWarnings("unused")
        Class<?> T();

        @SuppressWarnings("unused")
        int N();
    }

    @SuppressWarnings("WeakerAccess")
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD})
    @Indy(
            bootstrap = @Bootstrap(clazz = ConstantBootstraps.class, method = "bootstrap"),
            method = "GET:ELEMENT:pages")
    public @interface GetPage {
    }
}
