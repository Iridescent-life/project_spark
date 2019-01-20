package org.qianfeng.qftfmp.targetanalyse.monitoranalyse

import org.qianfeng.qftfmp.utils.SparkUtils

/*
 *  卡扣分析：
 *  分析指标：
 *    车流量最多的TonN卡扣号
 */
object TopNMonitorOfCarFlow {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val spark = SparkUtils.getSparkSession("MonitorAnalyze", "spark://192.168.226.101:7077")
    val sc = spark.sparkContext

    //车流量最多的TonN卡扣号
    val topNMonitor = sc.textFile("/qftfmp/basic/monitor_flow_action")

    topNMonitor.map(x => {
      val arr = x.split("\001")
      (arr(1), 1)
    }).reduceByKey(_ + _)
      .sortBy(x => x._2)
      .map(x => {
        //卡扣 + 车流量
        x._1 + "\001" + x._2
      }).saveAsTextFile("/qftfmp/result/monitorAnalyze/topNMonitorOfCarFlow")
  }
}
