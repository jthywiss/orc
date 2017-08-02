#! /bin/bash
ROOTDIR="$(readlink -f "$(dirname "$0")/..")"

. $ROOTDIR/PorcE/benchmark-localconfig.sh
if [ "$GRAALHOME" = "" ]; then
    echo "ERROR: benchmark-localconfig.sh (in $ROOTDIR/PorcE) must set GRAALHOME to the absolute path to the GraalVM JRE. Usually .../graalvm-0.xx/jre."
    exit 1
fi

SCALALIBS=$ROOTDIR/OrcScala/lib/scala-library.jar:$ROOTDIR/OrcScala/lib/scala-parser-combinators_2.12-1.0.5.jar:$ROOTDIR/OrcScala/lib/scala-reflect.jar:$ROOTDIR/OrcScala/lib/scala-xml_2.12-1.0.6.jar
ORCSCALALIBS=$ROOTDIR/OrcScala/build/orc-2.1.1.jar:$ROOTDIR/OrcScala/lib/swivel_2.12-0.0.1.jar
ORCTESTSLIBS=$ROOTDIR/OrcTests/build:$ROOTDIR/OrcTests/tools/junit-4.5.jar

PORCELIBS=$ROOTDIR/PorcE/bin
#:$GRAALHOME/lib/truffle/tuffle-api.jar:$GRAALHOME/lib/jvmci/graal.jar

GRAALBOOTLIBS="$GRAALHOME/lib/boot/graal-sdk.jar:$GRAALHOME/lib/truffle/truffle-api.jar:$GRAALHOME/lib/jvmci/graal.jar" 
GRAALARGS="-XX:-UseJVMCIClassLoader"

GRAALJAVA="$GRAALHOME/bin/java $GRAALARGS"
NORMALJAVA=java

CPUS=8
NRUNS=25
TIMEOUT=15

BATCHNAME="$(hostname)_$(date +"%Y%m%d-%H%M")"

echo "NOTE: Don't forget to build everything in eclipse first. Ideally with a clean git status."
echo "NOTE: Don't forget to set CPUs to a fixed frequency!"
sleep 2

for config in     \
                  "porceinterpO2%$NORMALJAVA%orc%porc%2" \
                  "porcegraalO2%$GRAALJAVA%orc%porc%2"   \
                  "porceinterpO3%$NORMALJAVA%orc%porc%3" \
                  "porcegraalO3%$GRAALJAVA%orc%porc%3"   \
                  "token%$NORMALJAVA%orc%token%3"        \
                  "scala%$NORMALJAVA%scala%token%3"      ; do
    IFS='%' read -r -a configs <<< "$config"
    NAME="${configs[0]}"
    JAVA="${configs[1]}"
    SET="${configs[2]}"
    BACKEND="${configs[3]}"
    OPT="${configs[4]}"
    
    cd $ROOTDIR/OrcTests

    echo "$config" > $ROOTDIR/benchmark_${BATCHNAME}_${NAME}.log    
    echo "Options: -O $OPT -S $SET -B $BACKEND -t $TIMEOUT -c $CPUS -r $NRUNS -o $ROOTDIR/benchmark_data_${BATCHNAME}_${NAME}.tsv" >> $ROOTDIR/benchmark_${BATCHNAME}_${NAME}.log    

    echo "Starting $(echo "$config" | tr "%" " ")"
    echo "Benchmark data: $ROOTDIR/benchmark_data_${BATCHNAME}_${NAME}.tsv"
    echo "Run log: $ROOTDIR/benchmark_${BATCHNAME}_${NAME}.log"
    echo 
    (time $JAVA -Xbootclasspath/a:"$GRAALBOOTLIBS" -classpath "$SCALALIBS:$ORCSCALALIBS:$ORCTESTSLIBS:$PORCELIBS" orc.test.BenchmarkTest \
            -O $OPT -S $SET -B $BACKEND -t $TIMEOUT -c $CPUS -r $NRUNS \
            -o $ROOTDIR/benchmark_data_${BATCHNAME}_${NAME}.tsv 2>&1 | tee -a $ROOTDIR/benchmark_${BATCHNAME}_${NAME}.log) \
            || exit 2
    echo "Finished $(echo "$config" | tr "%" " ")"
done


