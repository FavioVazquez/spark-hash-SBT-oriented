Disclaimer
==============================
This is my version of mrsqueeze's spark-hash but SBT oriented, all the credit of code and logic goes to him. This uses the sbt environment instead of the maven's environment.
The original repo is in [@mrsqueeze](https://github.com/mrsqueeze/) github, https://github.com/mrsqueeze/spark-hash

**Note** = In Globals.scala you should change the default domains to your
master spark domain (if using mesos) and your HDFS master domain. 

#### Project's GitHub pages: http://faviovazquez.github.io/spark-hash-SBT-oriented
----------------------------------------------------------------------

Spark-Hash
==============================

Locality sensitive hashing for [Apache Spark](http://spark.apache.org/).
This implementation was largely based on the algorithm described in chapter 3 of [Mining of Massive Datasets](http://mmds.org/) with some modifications for use in spark.

##### Please enter in the [Project's GitHub pages](http://faviovazquez.github.io/spark-hash-SBT-oriented) for more information :)

SBT environment
==============================

The steps to use this repo with SBT are the following:

1.- Make sure you have the latest sbt installed. For Ubuntu and other Debian-based distributions:

Ubuntu and other Debian-based distributions use the DEB format, but usually you don’t install
your software from a local DEB file. Instead they come with package managers both for the 
command line (e.g. apt-get, aptitude) or with a graphical user interface (e.g. Synaptic).
Run the following from the terminal to install sbt (You’ll need superuser privileges to do so, 
hence the sudo).

	echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
	sudo apt-get update
	sudo apt-get install sbt
	
2.- Add the Spark JAR to your project. To do this copy the JAR from your local Spark folder to
a folder in the project main tree. By example (assuming your local spark is in /opt and your
project is in Documents/myproject):

	sudo cp /opt/spark/lib/Spark.jar /home/username/Documents/myproject/lib
	
3.- In the project's folder run SBT assembly

	sbt assembly
	
4.- I strongly recommend that you upload the data folder to HDFS if you want to use the 
example given data in cluster mode, or you'll going to have to copy the project folder to the other datanodes. 
To do so use (to place it in the root folder):

	hadoop fs -put data/ /
	
5.- If you are using Spark in cluster (with Mesos) mode you can use spark-submit to run the code (in the Globals is defined  a generic domain for Spark Master using Apache Mesos and and also a generic HDFS domain, please change the masters according to your personalized domain): 

	spark-submit --class="package.classname" target/scala-2.10/spark-hash.jar
	
6.- To run it in local mode, just comment the setMaster(Globals.masterSpark) and uncomment the 
set.Master("local") line.

- If you want tu use the default Spark distribution in the maven repositories just add to the 
build.sbt the following lines in the libraryDependencies section, with provided if you have
uploaded your spark dist to HDFS, and without it if you have not:

	"org.apache.spark" % "spark-core_2.10" % "1.4.0" % "provided"
	
	"org.apache.spark" %% "spark-sql"  % "1.4.0" % "provided"

Example Data
-----
We executed some nmap probes and now would like to group IP addresses with similar sets of 
exposed ports. An small example of this dataset has been provided in data/sample.dat for 
illustration. 

As part of the preprocessing which was performed on nmap xml files, we flattened IP addresses
with identical port sets. The flattening did not factor the time when the port was open. 

To run SparkHashTry you have to specify the class in the spark-submit (if running in cluster mode)

	spark-submit --class="spark.hash.SparkHashTry" target/scala-2.10/spark-hash.jar
	

In the SparkHashTry example we explore the data, and later we will use the Hasher and LSH to
make use of the great mrsqueeze code.
	
	val port_set: RDD[(List[Int],Int)] = sc.objectFile(Globals.masterHDFS+"/data/sample.dat")
	port_set.take(5).foreach(println)
	
Result:

	(List(21, 23, 443, 8000, 8080),1)
	(List(80, 3389, 49152, 49153, 49154, 49155, 49156, 49157),9)
	(List(21, 23, 80, 2000, 3000),13)
	(List(1723),1)
	(List(3389),1)

Each row in the RDD is a Tuple2 containing the open ports alone with the number of distinct IP addresses that had those ports. In the above example, 13 IP addresses had ports 21, 23, 80, 2000, 3000 opened.

Count the RDD for the total dataset size:
	
	  println(port_set.count())
	 
Result:

	261
	
Count the total number of IP addresses that contributed to this dataset:

	println(port_set.map(x => x._2).reduce(_ + _))

Result:
	
	2273

Show the top five port sets sorted by IP count:

	port_set.sortBy(_._2, false).take(5).foreach(println)
	
Result:
	
	(List(21, 23, 80),496)
	(List(22, 53, 80),289)
	(List(80, 443),271)
	(List(80),228)
	(List(22, 53, 80, 443),186)

Filter the dataset to drop port sets smaller than 2. Show the top 5 results:

	val port_set_filtered = port_set.filter(tpl => tpl._1.size >= 2)
	port_set_filtered.sortBy(_._2, false).take(5).foreach(println)
	
Result:

	(List(21, 23, 80),496)
	(List(22, 53, 80),289)
	(List(80, 443),271)
	(List(22, 53, 80, 443),186)
	(List(21, 22, 23, 80, 2000),73)

Show the top three port sets by set size:

	port_set.sortBy(_._1.size, false).take(3).foreach(println)
	
Result:
	
	(List(13, 21, 37, 80, 113, 443, 1025, 1026, 1027, 2121, 3000, 5000, 5101, 5631),1)
	(List(21, 80, 443, 1723, 3306, 5800, 5900, 8081, 49152, 49153, 49154, 49155),1)
	(List(21, 22, 26, 80, 110, 143, 443, 465, 587, 993, 995, 3306),3)


Implementation Details
-----

Implementation of LSH follows the rough steps

1. minhash each vector some number of times. The number of times to hash is an input parameter.
 The hashing function is defined in spark.hash.Hasher. Essentially each element of the input vector is 
 hashed and the minimum hash value for the vector is returned. Minhashing produces a set of 
 signatures for each vector.
2. Chop up each vector's minhash signatures into bands where each band contains an equal 
number of signatures. Bands with a greater number of signatures will produce clusters with 
*greater* similarity. A greater number of bands will increase the probabilty that similar 
vector signatures  hash to the same value.
3. Order each of the vector bands such that for each band the vector's data for that
 band are grouped together. 
4. Hash each band and group similar values. These similar values for a given band 
that hash to the same value are added to the result set.
5. Optionally filter results. An example operation would be to filter out  singleton sets.

#### Example

Input data

1. [21, 25, 80, 110, 143, 443]
2. [21, 25, 80, 110, 143, 443, 8080]
3. [80, 2121, 3306, 3389, 8080, 8443]
4. [13, 17, 21, 23, 80, 137, 443, 3306, 3389]

Let's hash each vector 1000 times. To do this we'll need to create 1000 hash functions and
 minhash each vector 1000 times.

1. 1000 minhash signatures
2. 1000 minhash signatures
3. 1000 minhash signatures
4. 1000 minhash signatures

Now we want to chop up the signatures into 100 bands where each band will have 10 elements.

1. band 1 (10 elements), band 2 (10 elements), ... , band 100 (10 elements)
2. band 1 (10 elements), band 2 (10 elements), ... , band 100 (10 elements)
3. band 1 (10 elements), band 2 (10 elements), ... , band 100 (10 elements)
4. band 1 (10 elements), band 2 (10 elements), ... , band 100 (10 elements)

For each of the 4 sets of bands, group all of band 1, band 2, .... band 4

	band 1: vector 1 (10 elements), ... , vector 4 (10 elements) 

	band 2: vector 1 (10 elements), ... , vector 4 (10 elements) 
	
	band 100: vector 1 (10 elements), ... , vector 4 (10 elements) 

For each band, hash each of 10 element signatures.

	band 1: 10, 10, 3, 4

	band 2: 6, 5, 2, 4
	
	band 100: 1, 2, 3, 8

Group identical values within each band. This correspond to the similar clusters. In the above example, only two vectors (1 and 2) are deemed similar.

	model.clusters.foreach(println)
	(0,CompactBuffer((65535,[21,25,80,110,143,443],[1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,25,80,110,143,443,8080],[1.0,1.0,1.0,1.0,1.0,1.0,1.0])))


Usage
-----

The previously described data can easily be converted into Spark's SparseVector class with the open port corresponding to an index in the vector. With that in mind, there are a few domain specific tunables that will need to be set prior to running LSH

- *p* - a prime number > the largest vector index. In the case of open ports, this number is set to 65537.

- *m* - the number of "bins" to hash data into. Smaller numbers increase collisions. We use 1000.

- *numRows* - the total number of times to minhash a vector. *numRows* separate hash functions are generated. Larger numbers produce more samples and increase the likelihood similar vectors will hash together. 

- *numBands* - how many times to chop *numRows*. Each band will have *numRows*/*numBand* hash signatures. The larger number of elements the higher confidence in vector similarity. 

- *minClusterSize* - a post processing filter function that excludes clusters below a threshold.


### Driver Class
------

Executing a driver in local mode. This executes spark.hash.OpenPortDriver and saves the
resulting cluster output to the file results.csv. In this case 80% of the data is sampled
and spread over 8 partitions. For normal use the driver will need to be modified to handle
data load, requisite transforms, and parameter tuning. Also, for some datasets it may not be 
practical to save all results to a local driver.

- To run in local mode comment the setMaster(Globals.masterSpark) line and uncomment the 
setMaster("local"). To run in cluster mode, do the opposite (cluster mode is the default)

		spark-submit --class="spark.hash.OpenPortApp" target/scala-2.10/spark-hash.jar hdfs://yourdomain.com:8020/data/sample.dat

Result:

	Usage: OpenPortApp <file> <partitions> <data_sample>

- To sample (80%) and spread over 8 partitions:

		spark-submit --class="spark.hash.OpenPortApp" target/scala-2.10/spark-hash.jar hdfs://yourdomain.com:8020/data/sample.dat 8 0.5


### Finding similar sets for a new point
-------

This is implemented in the JaccardTry.scala file, to run:

	spark-submit --class="spark.hash.JaccardTry" target/scala-2.10/spark-hash.jar 

---

	val np = List(21, 23, 80, 2000, 8443)
	val nv = Vectors.sparse(65535, np.map(x => (x, 1.0))).asInstanceOf[SparseVector]
	
	//use jaccard score of 0.50 across the entire cluster. This may be a bit harsh for large tests.
	val sim = lsh.compute(nv, model, 0.50)
	
	println(sim.count())
	
	sim.collect().foreach(println)
---

Result:
	
	(9,List((65535,[21,22,23,80,2000,3389,8000],[1.0,1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,22,23,80,2000,3389],[1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8443],[1.0,1.0,1.0,1.0,1.0])))
	(4,List((65535,[21,23,80,81,2000],[1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8081],[1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8443],[1.0,1.0,1.0,1.0,1.0])))
	(5,List((65535,[21,22,23,80,1723,2000,8000],[1.0,1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,22,23,80,1723,2000],[1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8443],[1.0,1.0,1.0,1.0,1.0])))
	(6,List((65535,[21,22,23,53,80,2000,8000],[1.0,1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,22,23,53,80,2000],[1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8443],[1.0,1.0,1.0,1.0,1.0])))
	(7,List((65535,[21,22,23,80,554,2000],[1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,22,23,80,554,2000,8000],[1.0,1.0,1.0,1.0,1.0,1.0,1.0]), (65535,[21,23,80,2000,8443],[1.0,1.0,1.0,1.0,1.0])))


### Tuning Results
---------

As described in the MMDS book, LSH can be tuned by adjusting the number of rows and bands such that:

	threshold = Math.pow(1/bands),(1/(rows/bands))
	
Naturally, the number of rows, bands, and the resulting size of the band (rows/bands) dictates the quality of results yielded by LSH. Higher thresholds produces clusters with higher similarity. Lower thresholds typically produce more clusters but sacrifices similarity. 

Regardless of parameters, it may be good to independently verify each cluster. One such verification method is to calculate the jaccard similarity of the cluster (it is a set of sets). Implementation of jaccard similarity is provided in LSH.jaccard.
