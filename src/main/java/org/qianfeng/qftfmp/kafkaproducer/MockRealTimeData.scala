package org.qianfeng.qftfmp.kafkaproducer

import java.util.Properties

import org.apache.kafka.clients.producer.{KafkaProducer, Producer, ProducerRecord}
import scala.util.Random
import org.qianfeng.qftfmp.utils.DateUtils
import org.qianfeng.qftfmp.utils.StringUtils

import scala.util.Random

/**
  * @Auther: xinyu
  * @Date: 2019/1/16 08:55
  * @Description:
  */
object MockRealTimeData {
  def main(args: Array[String]): Unit = {
    val random = new Random
    val location = List("京", "豫", "鲁", "京", "京", "京", "沪", "京", "京", "深")
    //使用java.util.Properties类的对象来封装一些连接kafka必备的配置属性
    val prop = new Properties
    prop.put("bootstrap.servers", "hui:9092")
    //必须设置，就算打算只发送值内容
    prop.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    //指定的类会将值序列化
    prop.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    //指定多少分区副本收到消息，生产者才会认为消息写入成功
    prop.put("acks", "all")
    //声明生产者对象
    var producer: Producer[String, String] = null
    producer = new KafkaProducer(prop)

    try {
      while (true) {
        val date = DateUtils.getTodayDate
        var baseActionTime = date + " " + StringUtils.fulfuill(random.nextInt(24) + "")
        baseActionTime = date + " " + StringUtils.fulfuill((baseActionTime.split("")(1).toInt + 1) + "")
        val actionTime = baseActionTime + ":" + StringUtils.fulfuill(random.nextInt(60) + "") + ":" + StringUtils.fulfuill(random.nextInt(60) + "")
        val monitorId = StringUtils.fulfuill(4, random.nextInt(9) + "")
        val car = location(random.nextInt(10)) + (65 + random.nextInt(26)).asInstanceOf[Char] + StringUtils.fulfuill(5, random.nextInt(99999) + "")
        val speed = random.nextInt(260) + ""
        val roadId = random.nextInt(50) + 1 + ""
        val cameraId = StringUtils.fulfuill(5, random.nextInt(9999) + "")
        val areaId = StringUtils.fulfuill(2, random.nextInt(8) + "")
        //发送消息
        producer.send(new ProducerRecord("kafka", date + "\001" + monitorId + "\001" + cameraId + "\001" + car + "\001" + actionTime + "\001" + speed + "\001" + roadId + "\001" + areaId))
        //发送一条信息，打印一次提示信息
        println(date + "\001" + monitorId + "\001" + cameraId + "\001" + car + "\001" + actionTime + "\001" + speed + "\001" + roadId + "\001" + areaId)
        Thread.sleep(4000)
      }
    }
    //关闭资源
    producer.close()
  }
}
