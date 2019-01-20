package org.qianfeng.qftfmp.targetanalyse.trafficanalyse

import java.text.SimpleDateFormat

import org.qianfeng.qftfmp.utils.SparkUtils

import scala.collection.mutable

/**
  * 交通运行分析
  * 指标：随机抽取车辆
  *   在一天中要随机抽取100辆车，抽取的车辆可以权威代表当天交通运行情况。
  */
object RandomDrawingSample {
  def main(args: Array[String]): Unit = {

    System.setProperty("HADOOP_USER_NAME", "root")
    val spark = SparkUtils.getSparkSession("Questions", "spark://192.168.226.101:7077")

    //dataRDD中存放了指定时间范围内的数据
    val dataRDD = spark.sparkContext.textFile("/qftfmp/basic/monitor_flow_action")
      .map(everyLinesArr => {
        val everyFieldsArr = everyLinesArr.split("\001")
        (everyFieldsArr(0), everyFieldsArr(1), everyFieldsArr(2), everyFieldsArr(3),
          everyFieldsArr(4), everyFieldsArr(5), everyFieldsArr(6), everyFieldsArr(7))
      }).filter(x => {
          val time = x._5
          val startDateStr = "2019-01-15 00:00:00"
          val endDateStr = "2019-01-16 00:00:00"
          val newTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime
          val startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startDateStr).getTime
          val endTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(endDateStr).getTime
          startTime < newTime && newTime < endTime
        })

    //用于存放  (date,map(hour,count))
    val map = new mutable.HashMap[String, Long]()
    val mapBroadcast = spark.sparkContext.broadcast(map)

    //用于存放 (date，map(hour，rate)
    val mapHourCarRate = new mutable.HashMap[String, Double]()
    val mapHourCarRateBroadcast = spark.sparkContext.broadcast(mapHourCarRate)

    //100可为arg()  将参数封装为广播变量
    val carBroadcast = spark.sparkContext.broadcast(100)

    val rdd2 = dataRDD.map(ele => {
      val date = ele._5   //2019-01-15 10:10:10
      val date_hour = date.substring(0, 13) //2019-01-15 10
      //2019-01-15 10 ，车牌号
      (date_hour, ele._4)
    }).distinct()

    //  算出每小时车流量的比例
    val dateHourCount = rdd2.countByKey().map(ele => {
      val date_hour_fields = ele._1.split(" ")
      val date = date_hour_fields(0) //2019-01-15
      val hour = date_hour_fields(1) //10
      mapBroadcast.value.put(hour, ele._2)
      (date, mapBroadcast.value)//（date，(hour,count))
    })

    //统计一天中的车流量
    val dayCarCount = mapBroadcast.value.values.sum

    //我们得到一天的车流量后，需要计算采样比例(100/当天的车流量)
    //事实上，我们需要将每个小时的车流量乘以这个采样比例
    //比如说：一共大约要抽取100辆车
    // 05点的需要抽取的车辆为  100 * (05点的车流量/当天的车流量)
    // 稍微转化一下。即可得到  05点的车流量 * (100 /当天的车流量)
    // 将(100 /当天的车流量)这个采样比例给提取出来。
    // 并配合hour放入map当中， mapHourCarRateBroadcast(hour,采样比例)
    // 最后通过sampleByKey（即hour）进行抽样。
    // 这样可以保证从一天中按各小时车流量抽取一定的车辆，并且总量约为100
    for (hour <- mapBroadcast.value.keySet) {
      mapHourCarRateBroadcast.value.put(hour, (carBroadcast.value.toDouble / dayCarCount.toDouble).formatted("%.5f").toDouble)
    }

    //根据hour实现比例抽样
    val rdd3 = rdd2.map(ele => {
      val date_hour_fields = ele._1.split(" ")
      val hour = date_hour_fields(1)
      //10，car(车牌号)
      (hour, ele._2)
    }).sampleByKey(false, mapHourCarRateBroadcast.value, 1)
      .groupByKey()//(10,iterable(car,car,car...))

    // 包含了所有抽样后得到的car，将其格式换成二元组，以用于join
    val rdd4 = rdd3.flatMap(_._2).map((_,0))

    //下面的支线
    val rdda = dataRDD.map(ele => {
      //car,row
      (ele._4, ele)
    })

    //(car,(0,row))
    val rddb = rdd4.join(rdda)


    //将抽样后的车的所有信息拼接在一起，表示这个车的行车轨迹
    val rddc = rddb.map(ele => {
      //(car,row) 通过join后，只保留了抽样后得到的car的数据
      (ele._1, ele._2._2)
    }).groupByKey()//(car,iterable(row,row,row...))
      .map(x => {
        val list = x._2.toList
        val buffer = new StringBuffer()
        for (field <- list) {
          buffer.append("," + field)
        }
        //(car,该车的行车路线记录)
        x._1 + "\001" + buffer.toString.substring(1)
      }).saveAsTextFile("/qftfmp/result/trafficAnalyse/randomDrawingSample")
  }
}
