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
package com.sstewartgallus.peacod.runtime;

import jdk.dynalink.Operation;
import jdk.dynalink.StandardNamespace;
import jdk.dynalink.StandardOperation;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.support.Guards;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;

@FunctionalInterface
interface GetPage {
    static MethodHandle toHandle(GetPage g) {
        return ContextImpl.GET_PAGE.bindTo(g);
    }

    @SuppressWarnings("unused")
    Page getPage(Context context, int pageId) throws Relink;
}

@FunctionalInterface
interface GetInt {
    static MethodHandle toHandle(GetInt g) {
        return ContextImpl.GET.bindTo(g);
    }

    @SuppressWarnings("unused")
    int getInt(Context context, int addr) throws Relink;
}

@FunctionalInterface
interface SetInt {

    static MethodHandle toHandle(SetInt g) {
        return ContextImpl.SET.bindTo(g);
    }

    @SuppressWarnings("unused")
    void setInt(Context context, int addr, int value) throws Relink;
}

abstract class PageType<P> {
    abstract P anonymousPage();

    abstract int getInt(P page, int index);

    abstract void setInt(P page, int index, int value);

    public abstract Page box(P page);
}

final class ByteBufferPage extends PageType<ByteBuffer> {
    static final PageType TYPE = new ByteBufferPage();
    private static final VarHandle BYTE_BUFFER_AS_INTS = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());

    private ByteBufferPage() {

    }

    @Override
    ByteBuffer anonymousPage() {
        // FIXME: handle alignment
        return ByteBuffer
                .allocateDirect(Context.PAGE_SIZE)
                .order(ByteOrder.nativeOrder());
    }

    @Override
    int getInt(ByteBuffer page, int index) {
        return (int) BYTE_BUFFER_AS_INTS.get(page, index);
    }

    @Override
    void setInt(ByteBuffer page, int index, int value) {
        BYTE_BUFFER_AS_INTS.set(page, index, value);
    }

    @Override
    public Page box(ByteBuffer page) {
        return null;
    }
}

final class BytePage extends PageType<byte[]> {
    static final BytePage SINGLETON = new BytePage();
    private static final VarHandle BYTES_AS_INTS = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());
    private static final int ALIGNMENT_OFFSET = ByteBuffer.wrap(new byte[0]).
            alignmentOffset(0, 8);

    private BytePage() {
    }

    @Override
    byte[] anonymousPage() {
        return new byte[ALIGNMENT_OFFSET + Context.PAGE_SIZE];
    }

    @Override
    int getInt(byte[] page, int index) {
        return (int) BYTES_AS_INTS.get(page, ALIGNMENT_OFFSET + index);
    }

    @Override
    void setInt(byte[] page, int index, int value) {
        BYTES_AS_INTS.set(page, ALIGNMENT_OFFSET + index, value);
    }

    @Override
    public Page box(byte[] page) {
        return new Page() {
        };
    }
}


final class Relink extends Throwable {
    static final Relink RELINK = new Relink();

    private Relink() {
        super("", null, false, false);
    }
}

// A bit hacky but makes publishing more convenient
final class PageTables extends SwitchPoint {
    final Object[] pages;
    final PageType[] types;

    PageTables(PageType[] types, Object[] pages) {
        this.pages = pages;
        this.types = types;
    }
}

@SuppressWarnings("ALL")
final class ContextImpl extends Context {
    public static final MethodHandle GET_PAGE = functionalInterfaceHandle(GetPage.class);
    static final MethodHandle GET = functionalInterfaceHandle(GetInt.class);
    static final MethodHandle SET = functionalInterfaceHandle(SetInt.class);

    private static final Object[] NO_PAGES = new Object[0];
    private static final PageType<?>[] NO_TYPES = new PageType[0];
    private static final PageType<byte[]> BYTE_PAGE = BytePage.SINGLETON;

    // if we make this nonfinal we can do simple pointer check on the pages for mappings!
    // and update mmaps with atomics!
    private volatile PageTables pageTables;

    ContextImpl() {
        pageTables = new PageTables(NO_TYPES, NO_PAGES);
    }

    static <I> MethodHandle functionalInterfaceHandle(Class<I> iface) {
        var methods = Arrays.stream(iface.getDeclaredMethods())
                .filter((m) -> {
                    var mods = m.getModifiers();
                    return !Modifier.isStatic(mods);
                })
                .toArray(Method[]::new);
        if (methods.length != 1) {
            throw new IllegalArgumentException("not a single method interface");
        }
        var method = methods[0];

        MethodHandle handle;
        try {
            handle = lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return handle;
    }

    @Override
    public int anonymousPage() {
        var latestPageTables = pageTables;
        int n;
        synchronized (latestPageTables) {
            var heap = latestPageTables.pages;
            var types = latestPageTables.types;

            n = heap.length;
            heap = Arrays.copyOf(heap, n + 1, Object[].class);
            types = Arrays.copyOf(types, n + 1, PageType[].class);

            PageType type;
            if (true) {
                type = BYTE_PAGE;
            } else {
                type = ByteBufferPage.TYPE;
            }

            var page = type.anonymousPage();

            types[n] = type;
            heap[n] = page;
            pageTables = new PageTables(types, heap);
            SwitchPoint.invalidateAll(new SwitchPoint[]{latestPageTables});
        }
        return n << INDEX_BITS;
    }

    // Maybe add class info anyway?
    @Override
    GuardedInvocation link(Operation operation, Object[] namespaces, Object name, MethodType methodType, Object reciever, Object... arguments) {
        System.err.println("attempting to link " + operation + " " + name + " " + Arrays.toString(namespaces) + " " + Arrays.deepToString(arguments));

        String instruction;
        if (Objects.equals(StandardOperation.GET, operation)) {
            instruction = "get";
        } else if (Objects.equals(StandardOperation.SET, operation)) {
            instruction = "set";
        } else {
            return null;
        }

        if (!Arrays
                .stream(namespaces)
                .anyMatch((n) -> n.equals(StandardNamespace.ELEMENT))) {
            return null;
        }

        if (!(name instanceof String)) {
            return null;
        }
        String nameS = (String) name;
        switch (nameS) {
            default:
                return null;

            case "memory": {
                var latestPageTables = pageTables;
                MethodHandle result;
                switch (instruction) {
                    default:
                        throw new UnsupportedOperationException(instruction);
                    case "get":
                        return linkGetMemoryElement(methodType, latestPageTables, arguments);
                    case "set":
                        return linkSetMemoryElement(methodType, latestPageTables, arguments);
                }
            }
        }
    }

    private GuardedInvocation linkGetPage(MethodType methodType, PageTables latestPageTables, Object... arguments) {
        if (!methodType.equals(MethodType.methodType(Page.class, Context.class, int.class))) {
            return null;
        }
        var pageTypes = latestPageTables.types;
        var pages = latestPageTables.pages;

        var firstContext = (Context) arguments[0];
        var firstAddr = (int) arguments[1];

        var firstPageId = firstAddr >>> INDEX_BITS;

        var firstPageType = pageTypes[firstPageId];

        // page id isn't always constant though...
        var result = GetPage.toHandle((context, pageId) -> {
            var pageType = pageTypes[pageId];
            if (firstPageType != pageType) {
                throw Relink.RELINK;
            }
            var page = pages[pageId];
            return firstPageType.box(page);
        });

        return new GuardedInvocation(result,
                Guards.asType(Guards.getIdentityGuard(this), methodType),
                latestPageTables, Relink.class);
    }

    private GuardedInvocation linkGetMemoryElement(MethodType methodType, PageTables latestPageTables, Object... arguments) {
        if (!methodType.equals(MethodType.methodType(int.class, Context.class, int.class))) {
            return null;
        }
        var pageTypes = latestPageTables.types;
        var pages = latestPageTables.pages;

        var firstContext = (Context) arguments[0];
        var firstAddr = (int) arguments[1];

        var firstPageId = firstAddr >>> INDEX_BITS;

        var firstPageType = pageTypes[firstPageId];

        var result = GetInt.toHandle((context, addr) -> {
            var pageId = addr >>> Context.INDEX_BITS;
            var index = addr & Context.INDEX_MASK;
            var pageType = pageTypes[pageId];
            if (firstPageType != pageType) {
                throw Relink.RELINK;
            }
            var page = pages[pageId];
            return firstPageType.getInt(page, index);
        });

        return new GuardedInvocation(result,
                Guards.asType(Guards.getIdentityGuard(this), methodType),
                latestPageTables, Relink.class);
    }

    private GuardedInvocation linkSetMemoryElement(MethodType methodType, PageTables latestPageTables, Object... arguments) {
        if (!methodType.equals(MethodType.methodType(void.class, Context.class, int.class, int.class))) {
            return null;
        }

        var pageTypes = latestPageTables.types;
        var pages = latestPageTables.pages;

        var firstContext = (Context) arguments[0];
        var firstAddr = (int) arguments[1];

        var firstPageId = firstAddr >>> INDEX_BITS;

        var firstPageType = pageTypes[firstPageId];

        var result = SetInt.toHandle((context, addr, value) -> {
            var pageId = addr >>> Context.INDEX_BITS;
            var index = addr & Context.INDEX_MASK;
            var pageType = pageTypes[pageId];
            if (firstPageType != pageType) {
                throw Relink.RELINK;
            }
            var page = pages[pageId];
            firstPageType.setInt(page, index, value);
        });
        return new GuardedInvocation(result,
                Guards.asType(Guards.getIdentityGuard(this), methodType),
                latestPageTables, Relink.class);
    }
}