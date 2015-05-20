import org.apache.spark.{SparkContext, SparkConf}

/**
 * Created by worker1 on 20/05/15.
 */
object SparkHashTry {
def main(args: Array[String]) {

  val conf = new SparkConf()
    .setMaster("local")
    //      .setMaster(Globals.masterSpark)
    .setAppName("Spark-Hash")
  val sc = new SparkContext(conf)

  
  }
}
