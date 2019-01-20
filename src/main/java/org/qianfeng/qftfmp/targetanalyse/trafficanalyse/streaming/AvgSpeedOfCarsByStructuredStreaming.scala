package org.qianfeng.qftfmp.targetanalyse.trafficanalyse.streaming

import java.sql.Timestamp

import org.apache.spark.sql.functions.window
import org.apache.spark.sql.streaming.Trigger
import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 实时道路拥堵情况分析
  *   分析指标：每隔5s 计算一次当前卡扣过去5分钟 所有车辆的平均速度。
  *   使用方法：StructuredStreaming
  */
object AvgSpeedOfCarsByStructuredStreaming {
  def main(args: Array[String]): Unit = {
    val spark = SparkUtils.getSparkSession("flowTopNInfo", "local[1]")
    val lines = spark.readStream.format("kafka")
      .option("kafka.bootstrap.servers", "hui:9092")
      .option("subscribe", "kafka")
      .option("includeTimestamp", true)
      .load()

    import spark.implicits._
    val wc = lines.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)", "CAST( timestamp AS TIMESTAMP)")
      .as[(String, String, Timestamp)]
      .map(x => {
        val fields = x._2.split("\001")
        val speed = fields(5).toInt
        val monitorId = fields(1)
        val timestamp = x._3
        (monitorId, speed, timestamp)
      }).toDF("monitorId", "speed", "timestamp").withWatermark("timestamp", "1 seconds")
      .groupBy($"monitorId", window($"timestamp", "300 seconds", "5 seconds"))
      .avg("speed")


    wc.writeStream
      .outputMode("append")
      .format("console") //控制台输出
      .option("truncate", "false")
      .trigger(Trigger.ProcessingTime(5000))
      .start
      .awaitTermination()
  }

}
