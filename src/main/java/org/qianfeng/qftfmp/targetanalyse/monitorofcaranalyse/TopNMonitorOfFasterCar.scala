package org.qianfeng.qftfmp.targetanalyse.monitorofcaranalyse

import org.qianfeng.qftfmp.utils.SparkUtils

/**
  * 卡扣车辆分析：
  * 需求指标:
  *  指标二：通过车辆速度相对比较快的topN卡扣
  *
  */
object TopNMonitorOfFasterCar {
  /**
    * 此样例类中封装了卡扣下各阶段速度下通过的车辆数目
    *
    * @param highSpeed   高速通过的车辆数目
    * @param middleSpeed 中速通过的车辆数目
    * @param normalSpeed 正常速通过的车辆数目
    * @param lowSpeed    低速通过的车辆数目
    */
  case class SpeedsOfMonitor(highSpeed: Int, middleSpeed: Int, normalSpeed: Int, lowSpeed: Int) extends Ordered[SpeedsOfMonitor] {
    //四次排序，我们通过依次比较不同速度下的车辆数量，判断出
    // 车辆通过速度相对较快的卡扣数量
    override def compare(that: SpeedsOfMonitor): Int = {
      if (this.highSpeed - that.highSpeed != 0)
        this.highSpeed.compareTo(that.highSpeed)
      else if (this.middleSpeed - that.middleSpeed != 0)
        this.middleSpeed.compareTo(that.middleSpeed)
      else if (this.normalSpeed - that.normalSpeed != 0)
        this.normalSpeed.compareTo(that.normalSpeed)
      else this.lowSpeed.compareTo(lowSpeed)
    }
  }
  
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val sc = SparkUtils.getSparkContext("Questions", "spark://192.168.226.101:7077")

    //读取指定路径的文件创建基础RDD对象
    val initialRDD = sc.textFile("/qftfmp/basic/monitor_flow_action")
    
    //指标：通过车辆速度相对比较快的topN卡扣
    initialRDD.map(perLine => {
      val fields = perLine.split("\\001")
      (fields(1), fields(5).toInt) //monitor_id,speed
    }).groupByKey()
      .map(speedsOfMonitor => {
        val monitor = speedsOfMonitor._1 //卡扣号
        val speedOfCars: List[Int] = speedsOfMonitor._2.toList //此卡扣下经过的所有车辆的车速集合
        var highSpeed = 0 //高速通过的车辆数目
        var middleSpeed = 0 //中速通过的车辆数目
        var normalSpeed = 0 //正常速通过的车辆数目
        var lowSpeed = 0 //低速通过的车辆数目

        for (speed <- speedOfCars) {
          if (speed < 60 && speed > 0)
            lowSpeed += 1
          else if (speed < 90)
            normalSpeed += 1
          else if (speed < 120)
            middleSpeed += 1
          else
            highSpeed += 1
        }
        //卡扣，自定义的对象
        (monitor, SpeedsOfMonitor(highSpeed, middleSpeed, normalSpeed, lowSpeed))
      }).sortBy(_._2, false)
      .map(_._1)//取出符合指标的卡扣信息
      .saveAsTextFile("/qftfmp/result/monitorOfCarAnalyse/topNMonitorOfCarSpeed")
  }
}
