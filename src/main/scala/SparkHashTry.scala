import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by Favio on 20/05/15.
 */
object SparkHashTry {
def main(args: Array[String]) {

  val conf = new SparkConf()
//    .setMaster("local")
    //      .setMaster(Globals.masterSpark)
    .setAppName("Spark-Hash")
  val sc = new SparkContext(conf)

  val port_set: RDD[(List[Int],Int)] = sc.objectFile("data/sample.dat")
  port_set.take(5).foreach(println)
  }
}
