package org.qianfeng.qftfmp.targetanalyse.monitorofcaranalyse

import org.apache.spark.rdd.RDD
import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 卡扣车辆分析：
  * 需求指标:
  *  指标一：统计topN卡扣下经过的所有车辆详细信息
  */
object DetailCarOfTopNMonitor {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sc = SparkUtils.getSparkContext("Questions", "spark://192.168.226.101:7077")

    //读取指定路径的文件创建基础RDD对象
    val initialRDD = sc.textFile("/qftfmp/basic/monitor_flow_action")

    //获取通过车辆数最多的topN卡扣，用于join
    val monitorOfTopN: Array[(String, Int)] = initialRDD
      .map(perLine => {
        val fields = perLine.split("\\001")
        (fields(1), 1) //monitor_id
      }).reduceByKey(_ + _)
      .sortBy(_._2, false)
      .collect()
      .take(5) //这里取top5卡扣

    //通过车辆数最多的topN卡扣
    val topNOfCar: RDD[(String, Int)] = sc.parallelize(monitorOfTopN)

    //中间RDD，用于join操作
    val middleRDD = initialRDD.map(perLine => {
      val fields = perLine.split("\\001")
      //monitor_id,车牌号
      (fields(1), fields(3))
    })

    //指标：统计topN卡扣下经过的所有车辆详细信息
    topNOfCar
      .leftOuterJoin(middleRDD)
      .map(ele => {
        // 卡扣 + topN卡扣下经过的车辆信息
        ele._1 + "\001" + ele._2._2.get
      }).saveAsTextFile("/qftfmp/result/monitorOfCarAnalyse/topNMonitorOfCarInfo")
  }
}
