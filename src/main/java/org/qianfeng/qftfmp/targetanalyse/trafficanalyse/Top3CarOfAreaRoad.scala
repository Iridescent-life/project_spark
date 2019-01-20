package org.qianfeng.qftfmp.targetanalyse.trafficanalyse

import java.text.SimpleDateFormat

import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 交通运行分析
  *   指标：统计每个区域中车辆最多的前3道路
  */
object Top3CarOfAreaRoad {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val spark = SparkUtils.getSparkSession("Question11", "spark://192.168.226.101:7077")

    val dataRDD = spark.sparkContext.textFile("/qftfmp/basic/monitor_flow_action").map(everyLinesArr => {
      val everyFieldsArr = everyLinesArr.split("\001")
      (everyFieldsArr(0), everyFieldsArr(1),
        everyFieldsArr(2), everyFieldsArr(3), everyFieldsArr(4), everyFieldsArr(5), everyFieldsArr(6),
        everyFieldsArr(7))
    }).filter(x => {
        val time = x._5
        val startDateStr = "2019-01-14 00:00:00"
        val endDateStr = "2019-01-19 00:00:00"
        val newTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime
        val startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startDateStr).getTime
        val endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endDateStr).getTime
        startTime < newTime && newTime < endTime
      })

    //((25,01),CompactBuffer((2019-01-15,0005,18825,京D16060,2019-01-15 10:19:31,73,25,01)
    val res = dataRDD.groupBy(ele => (ele._7, ele._8))
      .sortBy(_._2.size, false)
      .take(3)
      .map(ele => {
        ele._1._1 + "\001" + ele._1._2 + "\001" + ele._2.size
      })

    spark.sparkContext.parallelize(res).saveAsTextFile("/qftfmp/result/Top3CarOfAreaRoad")
  }
}
