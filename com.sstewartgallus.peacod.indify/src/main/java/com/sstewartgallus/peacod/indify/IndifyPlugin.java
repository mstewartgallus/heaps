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
package com.sstewartgallus.peacod.indify;

import com.sstewartgallus.peacod.indify.annotations.Indy;
import com.sstewartgallus.peacod.indify.annotations.IndyRewrite;

import java.io.IOException;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.stream.Collectors;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.asm.MemberSubstitution.TypePoolResolver;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import net.bytebuddy.pool.TypePool;

import static net.bytebuddy.matcher.ElementMatchers.annotationType;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.declaresAnnotation;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

@SuppressWarnings("WeakerAccess")
public class IndifyPlugin implements Plugin {

    public IndifyPlugin() {
    }

    @Override
    public boolean matches(TypeDescription target) {
        return isAnnotatedWith(IndyRewrite.class).matches(target);
    }

    @Override
    public Builder<?> apply(Builder<?> bldr, TypeDescription td, ClassFileLocator cfl) {

        var platform = ClassFileLocator.ForClassLoader.ofPlatformLoader();

        var resolver = new TypePoolResolver.ForExplicitPool(TypePool.Default.of(new ClassFileLocator() {
            @Override
            public ClassFileLocator.Resolution locate(String string) throws IOException {
                var resolution = platform.locate(string);
                if (!(resolution instanceof ClassFileLocator.Resolution.Illegal)) {
                    return resolution;
                }
                return cfl.locate(string);
            }

            @Override
            public void close() throws IOException {
                platform.close();
            }
        }));

        var isIndy = annotationType(Indy.class);
        var isIndyAnnot = annotationType(declaresAnnotation(isIndy));

        return bldr.visit(MemberSubstitution
                .strict()
                .with(resolver)
                .method(declaresAnnotation(isIndyAnnot))
                .replaceWith((td1, md, tp) -> (td2, bce, paramTypes, retType)
                        -> {
                    var opAnnotation = bce
                            .getDeclaredAnnotations()
                            .filter(isIndyAnnot)
                            .getOnly();
                    var tag = opAnnotation
                            .getAnnotationType()
                            .getDeclaredAnnotations()
                            .filter(isIndy)
                            .getOnly()
                            .prepare(Indy.class)
                            .loadSilent();
                    var methodName = tag.method();

                    var bootstrapClass = tag.bootstrap().clazz();
                    var bootstrapMethod = tag.bootstrap().method();

                    Method bootstrap;
                    try {
                        bootstrap = bootstrapClass.getDeclaredMethod(bootstrapMethod, Lookup.class, String.class, MethodType.class, Object[].class);
                    } catch (NoSuchMethodException | SecurityException ex) {
                        throw new RuntimeException(ex);
                    }

                    var bootstrapDescription = new MethodDescription.ForLoadedMethod(bootstrap);

                    var params = new ArrayList<>();
                    for (var method : opAnnotation.getAnnotationType().getDeclaredMethods()) {
                        var val = opAnnotation.getValue(method).resolve();
                        if (val instanceof TypeDescription.ForLoadedType) {
                            val = ((TypeDescription.ForLoadedType) val).getDescriptor();
                        }
                        params.add(val);
                    }

                    return MethodInvocation
                            .invoke(bootstrapDescription)
                            .dynamic(methodName,
                                    retType.asErasure(),
                                    paramTypes
                                            .stream()
                                            .map(TypeDefinition::asErasure)
                                            .collect(Collectors.toList()),
                                    params);
                })
                .on(any()));
    }

    @Override
    public void close() {
        // nothing to do
    }
}
