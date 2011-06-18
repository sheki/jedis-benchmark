#!/usr/bin/env bash
for i in 1 16 32 64 128 256 ; do
    java  -Xms500M -Xmx2048M  -jar target/jedis-benchmark-1.0-jar-with-dependencies.jar  -t $i -c $i -s 500  &> benchmark.$i.log
done
