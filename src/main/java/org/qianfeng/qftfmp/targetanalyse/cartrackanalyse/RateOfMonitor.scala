package org.qianfeng.qftfmp.targetanalyse.cartrackanalyse

import java.text.SimpleDateFormat
import java.util

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable.ListBuffer

/**
  * 车辆轨迹分析：
  *   指标二：
  *   卡扣流量转换率
  *    一辆车的轨迹：
  *      0001->0002->0003->0001->0002->0004->0005->0001
  *      0001,0002----卡扣0001到卡扣0002 的车流量转化率：通过卡扣0001又通过卡扣0002的次数/通过卡扣0001的次数  2/3
  **      0001,0002,0003 ---- 卡扣0001,0002到0003的车辆转换率：通过卡扣0001,0002,0003的次数 /通过卡扣0001,0002
  **      0001,0002,0003,0004 -----卡扣0001,0002，0003到0004的车辆转换率：通过卡扣0001,0002,0003,0004的次数 /通过卡扣0001,0002,0003
  **      0001,0002,0003,0004,0005 -----卡扣0001,0002，0003,0004到0005的车辆转换率：通过卡扣0001,0002,0003,0004,0005的次数 /通过卡扣0001,0002,0003,0004的次数
  *    手动输入卡扣号：
  *      0001,0002,0003,0004,0005
  *      求：
  *        0001,0002
  *        0001,0002,0003
  *        0001,0002,0003,0004
  *        0001,0002,0003,0004,0005
  *
  *      粤A11111：
  *         ("0001"，100)
  *        ("0001,0002",30)
  *        ("0001,0002,0003",10)
  *      粤B22222：
  *        ("0001"，200)
  *        ("0001,0002",100)
  *        ("0001,0002,0003",70)
  *        ("0001,0002,0003,0004",10)
  */

object RateOfMonitor {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val conf: SparkConf = new SparkConf()
    conf.setMaster("spark://192.168.226.101:7077")
    conf.setAppName("name".getClass.getSimpleName)
    val sc = new SparkContext(conf)
    //设置参数
    val track = sc.getConf.get("args", "0001,0002,0003,0004,0005")

    val rdd = sc.textFile("/qftfmp/basic/monitor_flow_action").map(x => {
      val arr = x.split("\001")
      (arr(3), (arr(1), arr(4))) //(车，(卡扣,时间))
    }).groupBy(_._1) //(车，list（（车，（卡扣，时间）），）)
      .map(x => {
        val utils = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        (x._1, x._2.toList.sortWith((x, y) => if (utils.parse(x._2._2).compareTo(utils.parse(y._2._2)) < 0) true else false))
      }).map(x => {
          var str = ""
          for (elem <- x._2.map(_._2._1)) {
            str += elem + ","
          }
          (x._1, str) //车，卡扣 0003,0004
        })

    val per_tracks = track.split(",") //拼凑需求
    val questions = new ListBuffer[String]()
    var con = per_tracks(0)
    questions.append(con)

    for (i <- 1 to per_tracks.size - 1) {
      con = con + "," + per_tracks(i)
      questions.append(con)
    }

    val list = new util.ArrayList[RDD[(String, String, Int)]]()

    questions.map(x => {
      val t = sc.broadcast(x)
      rdd.map(y => {
        (y._1, t.value, (y._2.split(t.value).size - 1))
      })
    }).foreach(x => list.add(x))

    var rdd1 = list.get(0)
    for (i <- 1 to list.size() - 1) {
      rdd1 = rdd1.union(list.get(i))
    }

    rdd1.groupBy(_._1)
      .map(x => {
        val car = x._1
        val map = new util.TreeMap[String, String]()
        val lst = x._2.toList
        map.put(lst(0)._2, lst(0)._3.toDouble.formatted("%.2f"))
        for (t <- 1 to (lst.size - 1)) {
          if (lst(t)._3 == 0) {
            map.put(lst(t)._2, "0")
          } else if (lst(t - 1)._3 == 0) {
            map.put(lst(t)._2, "0")
          } else {
            val n = (lst(t)._3.toDouble / lst(t - 1)._3).formatted("%.2f")
            map.put(lst(t)._2, n)
          }
        }
        var str = ""
        import scala.collection.JavaConversions._
        for (key <- map.keySet().iterator()) {
          str = str + key + "=" + map.get(key) + ","
        }
        x._1 + "\001" + str.substring(0, str.length - 1)
    }).saveAsTextFile("/qftfmp/result/carTrackAnalyse/monitorFlowConversionRate")

  }
}
