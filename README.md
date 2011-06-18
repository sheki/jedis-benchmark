# Jedis Benchmark
Project to benchmark Redis using a Java Client [Jedis](https://github.com/xetorthio/jedis).

##Building
Download (or clone) the code and run `mvn install`.
It should build a jar called jedis-benchmark-1.0-jar-with-dependencies.jar.
##Usage
The program assumes defaults for all the values, which can be changed by using the given options.
-  -n : number of operations. Default is 100000.
-  -t : number of threads (concurrent clients).  Default is 1.
-  -c : number of Jedis connections. Default is 1.
-  -h : Host on which redis is running. Default is "localhost".
-  -p : Redis port. Default is 6379.
-  s  : Data size to be performed on the set operation. (currently only performs the set operation).


##TODO
- Currently only performs the set operations. Need to extend it to perform all opeartions supported by Jedis.

