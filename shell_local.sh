#!/usr/bin/env bash
$SPARK_HOME/bin/spark-shell \
  --master local[*] \
  --executor-memory 8G \
  --driver-memory 8G \
  --jars target/scala-2.10/spark-hash.jar $@
