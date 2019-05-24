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
package com.sstewartgallus.peacod.examples;

import com.sstewartgallus.peacod.indify.annotations.IndyRewrite;
import com.sstewartgallus.peacod.interop.ContextCalls;
import com.sstewartgallus.peacod.runtime.Context;
import com.sstewartgallus.peacod.runtime.Page;

import java.util.Random;

/**
 * @author sstewart-gallus
 */
@SuppressWarnings("EmptyMethod")
@IndyRewrite
class ExampleApp {

    private static final Context CONTEXT = Context.newInstance();

    private static final int ITER = 200000;
    private static final int M = 30;

    private static final boolean MEMTEST = true;
    private static int top = -1;
    private static int currentPage = -1;

    public static void main(String[] args) {
        var r = new Random();

        if (MEMTEST) {
            memtest();
        }

        var a = malloc(4 * (M * M));
        var b = malloc(4 * (M * M));
        var c = malloc(4 * (M * M));

        System.out.println("a = 0x" + Integer.toHexString(a));
        System.out.println("b = 0x" + Integer.toHexString(b));
        System.out.println("c = 0x" + Integer.toHexString(c));

        for (var ii = 0; ii < M; ++ii) {
            for (var jj = 0; jj < M; ++jj) {
                set(a, ii, jj, r.nextInt());
            }
        }

        for (var ii = 0; ii < M; ++ii) {
            for (var jj = 0; jj < M; ++jj) {
                set(b, ii, jj, r.nextInt());
            }
        }

        hotLoop(a, b, c);

        for (var ii = 0; ii < M; ++ii) {
            for (var jj = 0; jj < M; ++jj) {
                System.out.print("[" + G.get(CONTEXT, c + 4 * ii * M + 4 * jj) + "] ");
            }
            System.out.println();
        }

        free();
        free();
        free();
    }

    private static void hotLoop(int a, int b, int c) {
        for (var ii = 0; ii < ITER; ++ii) {
            mul(a, b, c);
            mul(c, a, b);
            mul(b, c, a);
        }
    }

    private static void mul(int a, int b, int c) {
        for (var ii = 0; ii < M; ++ii) {

            for (var kk = 0; kk < M; ++kk) {
                var sum = 0;
                for (int jj = 0; jj < M; ++jj) {
                    sum += G.get(CONTEXT, a + ii * M + jj) * G.get(CONTEXT, b + jj * M + kk);
                }
                G.set(CONTEXT, c + ii * M + kk, sum);
            }
        }
    }

    /*
        private static void mul(int a, int b, int c) {
            // matrix multiplication as we all know is defined as [AB]_ii_jj = A_ii \cdot B^T_jj
            var transposed = push(4 * M * M);
            try {
                var muls = push(4 * M);
                try {
                    transpose(transposed, b);
    // TODO: break into blocks?
                    for (var ii = 0; ii < M; ++ii) {
                        var aOffset = a + (4 * M) * ii;
                        for (var jj = 0; jj < M; ++jj) {
                            G.copy(CONTEXT, muls, transposed + 4 * jj);
                            G.mul(CONTEXT, muls, aOffset);
                            var sum = G.sum(CONTEXT, muls);
                            set(c, ii, jj, sum);
                        }
                    }
                } finally {
                    pop(muls, 4 * M);
                }
            } finally {
                pop(transposed, 4 * M * M);
            }
        }

        private static void transpose(int dest, int src) {
            for (int ii = 0; ii < M; ++ii) {
                G.scatter(CONTEXT, dest + 4 * ii, src + (4 * M) * ii);
            }
        }
    */
    private static void set(int matrix, int ii, int jj, int val) {
        G.set(CONTEXT, matrix + ii * (4 * M) + jj * 4, val);
    }

    private static void memtest() {
        var rnd = new int[100];
        var r = new Random();
        for (var ii = 0; ii < rnd.length; ++ii) {
            rnd[ii] = r.nextInt();
        }

        var a = malloc(4 * rnd.length);

        for (var ii = 0; ii < rnd.length; ++ii) {
            G.set(CONTEXT, a + 4 * ii, rnd[ii]);
        }
        for (var ii = 0; ii < rnd.length; ++ii) {
            var x = rnd[ii];
            var addr = a + 4 * ii;
            var y = G.get(CONTEXT, addr);
            if (x != y) {
                throw new Error("rnd[ii]=0x" + Integer.toHexString(x) + " /\\ methodArgument(a+ii)=0x" + Integer.toHexString(y) + " /\\ addr=0x" + Integer.toHexString(addr));
            }
        }

        free();
    }

    private static int malloc(int size) {
        if (size > Context.PAGE_SIZE) {
            throw new Error("unimplemented");
        }

        var remaining = Context.PAGE_SIZE - top;
        if (size > remaining || top < 0) {
            top = 0;
            currentPage = CONTEXT.anonymousPage();
            System.err.println("new page " + Integer.toHexString(currentPage));
        }
        int ptr = currentPage + top;
        top += size;
        return ptr;
    }

    private static void free() {
    }

    private interface G {

        @ContextCalls.Scatter(T = int.class, N = M, Stride = M)
        static void scatter(Context cntx, int dest, int src) {
            throw new Error("stub");
        }

        @ContextCalls.Sum(T = int.class, N = M)
        static int sum(Context cntx, int addr) {
            throw new Error("stub");
        }

        @ContextCalls.Set(T = int.class)
        static void set(Context cntx, int addr, int value) {
            throw new Error("stub");
        }

        @ContextCalls.GetPage
        static Page x(Context cntx, int pageId) {
            throw new Error("stub");
        }

        @ContextCalls.Get(T = int.class)
        static int get(Context cntx, int addr) {
            throw new Error("stub");
        }

        @ContextCalls.Mul(T = int.class, N = M)
        static void mul(Context cntx, int accum, int src) {
            throw new Error("stub");
        }

        @SuppressWarnings("SameParameterValue")
        @ContextCalls.Copy(T = int.class, N = M)
        private static void copy(Context cntx, int dest, int src) {
            throw new Error("stub");
        }
    }
}
