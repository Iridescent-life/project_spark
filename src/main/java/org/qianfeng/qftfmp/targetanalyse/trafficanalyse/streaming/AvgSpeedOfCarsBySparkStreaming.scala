package org.qianfeng.qftfmp.targetanalyse.trafficanalyse.streaming

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 实时道路拥堵情况分析
  * 分析指标：每隔5s 计算一次当前卡扣过去5分钟 所有车辆的平均速度。
  * 使用方法：SparkStreaming
  */
object AvgSpeedOfCarsBySparkStreaming {
  def main(args: Array[String]): Unit = {
    val sc = SparkUtils.getSparkContext("group5", "local[1]")
    val ssc = new StreamingContext(sc, Seconds(1))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "hui:9092,hui2:9092,hui3:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "group5",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val topics = Array("kafka")
    val stream = KafkaUtils.createDirectStream(
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    stream.map(x => {
      val fields = x.value().split("\001")
      val speed = fields(5)
      val monitorId = fields(1)
      (monitorId, speed.toInt)
    }).groupByKeyAndWindow(Seconds(3000), Seconds(5))
      .map(x => {
        val avgSpeed = x._2.sum / x._2.size
        (x._1, avgSpeed)
      }).print(10)

    ssc.start()
    ssc.awaitTermination()
  }
}
