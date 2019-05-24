#!/bin/sh
# cd com.sstewartgallus.peacod.repl && exec mvn dependency:properties exec:exec

M2="$HOME/.m2/repository"
JAVA="$JAVA_HOME/bin/java"
MODULES="$M2/com/sstewartgallus/peacod/peacod-examples/1.0-SNAPSHOT/peacod-examples-1.0-SNAPSHOT.jar"
while read II
do
	MODULES="$MODULES:$M2/$II"
done <<EOH
com/sstewartgallus/peacod/peacod-runtime/1.0-SNAPSHOT/peacod-runtime-1.0-SNAPSHOT.jar
com/sstewartgallus/peacod/peacod-interop/1.0-SNAPSHOT/peacod-interop-1.0-SNAPSHOT.jar
com/sstewartgallus/peacod/peacod-indify-plugin/1.0-SNAPSHOT/peacod-indify-plugin-1.0-SNAPSHOT.jar
net/bytebuddy/byte-buddy/1.9.7/byte-buddy-1.9.7.jar
org/ow2/asm/asm-tree/6.2/asm-tree-6.2.jar
org/ow2/asm/asm-analysis/6.2/asm-analysis-6.2.jar
org/ow2/asm/asm-util/6.2/asm-util-6.2.jar
org/ow2/asm/asm/6.2/asm-6.2.jar
org/antlr/antlr4-runtime/4.7/antlr4-runtime-4.7.jar
EOH
#exec $JAVA -Xjit:verbose='{compileStart|compileEnd|inlining}' --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.ExampleApp" "$@"
# exec $JAVA -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -XX:+PrintCompilation --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.ExampleApp" "$@"
# exec $JAVA -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.ExampleApp" "$@"
# exec $JAVA --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.Baseline" "$@"
# exec $JAVA -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -XX:+PrintCompilation --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.ExampleApp" "$@"
exec $JAVA --module-path $MODULES --module "com.sstewartgallus.peacod.examples/com.sstewartgallus.peacod.examples.ExampleApp" "$@"
