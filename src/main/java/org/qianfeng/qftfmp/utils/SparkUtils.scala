package org.qianfeng.qftfmp.utils

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

object SparkUtils {
  def getSparkContext(name: String, master: String): SparkContext = {
    val conf: SparkConf = new SparkConf()
    conf.setMaster(master)
    conf.setAppName(name.getClass.getSimpleName)
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")
    sc
  }

  def getSparkSession(appname: String, master: String): SparkSession = {
    val warehouse = "/user/hive/warehouse"
    val spark = SparkSession.builder()
      .enableHiveSupport()
      .appName(appname.getClass.getSimpleName)
      .config("spark.sql.warehouse.dir", warehouse)
      .master(master)
      .getOrCreate()

    spark.sparkContext.setLogLevel("WARN")
    spark
  }
}
