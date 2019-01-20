package org.qianfeng.qftfmp.targetanalyse.cartrackanalyse

import java.text.SimpleDateFormat

import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 车辆轨迹分析：
  *   指标一：过滤日期范围内 卡扣“0001”下 的车辆
  *           并根据这些车辆经过卡扣的时间，进行升序排序
  */

object GetCarByOrderedTime {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sc = SparkUtils.getSparkContext("group5", "spark://192.168.226.101:7077")

    //指标一：过滤日期范围内 卡扣“0001”下 的车辆
    //            并根据这些车辆经过卡扣的时间，进行升序排序
    sc.textFile("/qftfmp/basic/monitor_flow_action")
      .filter(x => {
        val arr = x.split("\001")
        arr(0).equals("2019-01-15") && arr(1).equals("0001")
      }).map(x => {
          val arr = x.split("\001")
        //日期 + 车牌号
          (arr(4), arr(3))
       }).sortBy(x => {
          val dateTime = x._1
          val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
          //将表示时间的字符串解析为Date类型，用于排序。(Date类型自带排序规则)
          sdf.parse(dateTime)
        }).map(x => {
        //日期 + 车牌号
          x._1 + "\001" + x._2
        }).saveAsTextFile("/qftfmp/result/carTrackAnalyse/carSortedByTime")
  }
}
