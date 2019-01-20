package org.qianfeng.qftfmp.data2hive

import org.qianfeng.qftfmp.utils.SparkUtils

object DataToHive {
  /*
    spark on  hive 直接创建hive表 将数据导入
   */
  def main(args: Array[String]): Unit = {

   val spark = SparkUtils.getSparkSession("DataToHive","local[*]")
    spark.sql("create database qftfmp")
    spark.sql("use  qftfmp")

    spark.sql("create  table   monitor_flow_action (date String,monitor_Id String,camera_Id String,car String" +
      ",action_Time String,speed String,road_Id String,area_Id String ) row format delimited fields terminated by \"\\001\"" )

    spark.sql("load  data local inpath '/home/ceshi/monitor_flow_action' into table monitor_flow_action")

    spark.sql("create  table monitor_camera_info(monitor_id String,camera_id String) " +
      "row format delimited fields terminated by \"\\001\"")

    spark.sql("load  data local inpath '/home/ceshi/monitor_camera_info' into table monitor_camera_info")

    spark.stop()
  }
}
