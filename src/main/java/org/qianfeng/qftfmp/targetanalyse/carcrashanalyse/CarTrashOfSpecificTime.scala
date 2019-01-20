package org.qianfeng.qftfmp.targetanalyse.carcrashanalyse

import java.text.SimpleDateFormat

import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 车辆碰撞分析:
  *   指标 ：获得指定时间段内发生车辆碰撞的车辆
  */
object CarTrashOfSpecificTime {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sc = SparkUtils.getSparkContext("Questions", "spark://192.168.226.101:7077")

    //读取指定路径的文件创建基础RDD对象
    val initialRDD = sc.textFile("/qftfmp/basic/monitor_flow_action")

    //指标 ：获得指定时间段内发生车辆碰撞的车辆
    val startTime = "2019-01-15 08:00:00"
    val endTime = "2019-01-15 08:10:00"
    val sf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val startTimeStamp = sf.parse(startTime).getTime //转化为时间戳
    val endTimeStamp = sf.parse(endTime).getTime

    //当指定范围时间内同一辆车出现在多个区域，我们认为该车出现车辆碰撞
    initialRDD.filter(perLine => {
      val fields = perLine.split("\\001")
      val actionTimeStamp = sf.parse(fields(4)).getTime //转化为时间戳
      actionTimeStamp <= endTimeStamp && actionTimeStamp >= startTimeStamp
    }).map(perLine => {
        val fields = perLine.split("\\001")
        val area = fields(7) //区域id
        val car = fields(3) //车牌号
        (car, area)
      }).distinct()
        .groupByKey()
        .filter(_._2.size > 1) //筛选出指定时间段内出现在多个区域的车辆，并显示它们出现的区域
        .map(x => {
          val car = x._1
          var areas = ""
          val itr = x._2.toIterator
          while (itr.hasNext) {
            areas = areas + itr.next() + ","
          }
          car + "\001" + areas.substring(0, areas.length - 1)
        }).saveAsTextFile("/qftfmp/result/carTrashAnalyse/carCrash")
  }
}
