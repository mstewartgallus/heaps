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

import jdk.dynalink.*;
import jdk.dynalink.support.ChainedCallSite;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;

public final class ConstantBootstraps {
    private static final DynamicLinker DYNALINKER;

    static {
        var mylinker = new Linker();

        var fact = new DynamicLinkerFactory();
        fact.setSyncOnRelink(true);
        fact.setPrioritizedLinkers(Collections.singletonList(mylinker));
        DYNALINKER = fact.createLinker();
    }


    // This method is looked up and called dynamically
    @SuppressWarnings("unused")
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Object... args) {
        var callSiteDescriptor = new CallSiteDescriptor(lookup, parseOperation(name, args), type);
        var callSite = new ChainedCallSite(callSiteDescriptor);
        return DYNALINKER.link(callSite);
    }

    private static Operation parseOperation(String operation, Object[] args) {
        var parts = operation.split(":");

        var base = parts[0];
        String[] namespacesStrings = null;
        if (parts.length > 1) {
            namespacesStrings = parts[1].split("\\|");
        }

        Object name = null;
        if (parts.length > 2) {
            name = parts[2];
        }

        Operation op = StandardOperation.valueOf(base);

        if (namespacesStrings != null) {
            var namespaces = new ArrayList<StandardNamespace>();
            for (var str : namespacesStrings) {
                StandardNamespace namespace;
                try {
                    namespace = StandardNamespace.valueOf(str);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                namespaces.add(namespace);
            }

            // Throw an error if we cannot find any namespaces we understand
            if (namespaces.isEmpty()) {
                throw new IllegalArgumentException(parts[1]);
            }

            op = op.withNamespaces(namespaces.toArray(new StandardNamespace[0]));
        }

        if (name != null) {
            op = op.named(name);
        }

        return op;
    }
}
