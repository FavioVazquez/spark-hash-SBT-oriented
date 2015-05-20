import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import globals.Globals

/**
 * Created by Favio on 20/05/15.
 */

object SparkHashTry {
def main(args: Array[String]) {

  val conf = new SparkConf()
    .setMaster("local")
//          .setMaster(Globals.masterSpark)
    .setAppName("Spark-Hash")
  val sc = new SparkContext(conf)

  val port_set: RDD[(List[Int],Int)] = sc.objectFile(Globals.masterHDFS+  "/data/sample.dat")

//  Print first five items
  port_set.take(5).foreach(println)
  println("===========================")

//  Count the RDD for the total dataset size:
  println(port_set.count())
  println("===========================")

//  Count the total number of IP addresses that contributed to this dataset:
  println(port_set.map(x => x._2).reduce(_ + _))
  println("===========================")

//  Show the top five port sets sorted by IP count:
  port_set.sortBy(_._2,false).take(5).foreach(println)
  println("===========================")

//  Filter the dataset to drop port sets smaller than 2. Show the top 5 results:
  val port_set_filtered = port_set.filter(tpl => tpl._1.size >= 2)
  port_set_filtered.sortBy(_._2, false).take(5).foreach(println)
  println("===========================")

//  Show the top three port sets by set size:
  port_set.sortBy(_._1.size,false).take(3).foreach(println)





  }
}
