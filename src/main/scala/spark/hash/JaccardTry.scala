package spark.hash

import org.apache.spark.mllib.linalg.{SparseVector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by Favio on 20/05/15.
 */

object JaccardTry {
  val conf = new SparkConf()
//    .setMaster("local")
    .setMaster(Globals.masterSpark)
    .setAppName("Spark-Hash")
  val sc = new SparkContext(conf)

  val port_set: RDD[(List[Int],Int)] = sc.objectFile(Globals.masterHDFS+  "/data/sample.dat")
  port_set.repartition(8)

  val port_set_filtered = port_set.filter(tpl => tpl._1.size > 3)

  val vctr = port_set_filtered.map(r => (r._1.map(i => (i, 1.0)))).map(a => Vectors.sparse(65535, a).asInstanceOf[SparseVector])

  val lsh = new  LSH(data = vctr, p = 65537, m = 1000, numRows = 1000, numBands = 25, minClusterSize = 2)

  val model = lsh.run

  val np = List(21, 23, 80, 2000, 8443)

  val nv = Vectors.sparse(65535, np.map(x => (x, 1.0))).asInstanceOf[SparseVector]

//  use jaccard score of 0.50 across the entire cluster. This may be a bit harsh for large tests.
  val sim = lsh.compute(nv,model,0.50)

  sim.collect().foreach(println)

  sc.stop()
}
