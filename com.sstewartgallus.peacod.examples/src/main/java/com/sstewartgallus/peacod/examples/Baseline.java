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

import java.util.Random;

class Baseline {

    private static final int ITER = 200000;
    private static final int M = 30;

    public static void main(String[] args) {
        var r = new Random();

        var a = new int[M][M];
        var b = new int[M][M];
        var c = new int[M][M];

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
                System.out.print("[" + get(c, ii, jj) + "] ");
            }
            System.out.println();
        }

    }

    private static void hotLoop(int[][] a, int[][] b, int[][] c) {
        for (var ii = 0; ii < ITER; ++ii) {
            mul(a, b, c);
            mul(c, a, b);
            mul(b, c, a);
        }
    }

    private static void mul(int[][] a, int[][] b, int[][] c) {
        for (var ii = 0; ii < M; ++ii) {
            var cRow = c[ii];
            var aII = a[ii];

            for (var kk = 0; kk < M; ++kk) {
                var sum = 0;
                for (int jj = 0; jj < M; ++jj) {
                    sum += aII[jj] * b[jj][kk];
                }
                cRow[kk] = sum;
            }
        }
    }

    private static int get(int[][] matrix, int ii, int jj) {
        return matrix[ii][jj];
    }

    private static void set(int[][] matrix, int ii, int jj, int val) {
        matrix[ii][jj] = val;
    }
}
