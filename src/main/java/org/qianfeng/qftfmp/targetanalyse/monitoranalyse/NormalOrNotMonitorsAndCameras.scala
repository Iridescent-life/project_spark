package org.qianfeng.qftfmp.targetanalyse.monitoranalyse

import org.qianfeng.qftfmp.utils.SparkUtils

import scala.collection.mutable.ArrayBuffer

/*
 *  卡扣分析：
 *  分析指标：
 * 	1、正常卡扣数量
 * 	2、异常卡扣数量
 * 	3、正常通道（此通道的摄像头运行正常）数，通道就是摄像头
 * 	4、异常卡扣数量中哪些摄像头异常，需要保存摄像头的编号
 */
object NormalOrNotMonitorsAndCameras {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME", "root")
    val spark = SparkUtils.getSparkSession("MonitorAnalyze", "spark://192.168.226.101:7077")
    val sc = spark.sparkContext

    val monitor_flow_action = sc.textFile("/qftfmp/basic/monitor_flow_action")
      .map(x => {
        val arr = x.split("\001")
        val monitor_id = arr(1)
        val camera_id = arr(2)
        (monitor_id, camera_id) // (卡扣id,摄像头id)
      }).groupByKey()
      .map(x => {
        val buffer = new ArrayBuffer[String]()
        val itr = x._2.toIterator //所有的摄像头id放入迭代器中
        while (itr.hasNext) {
          buffer += itr.next() // 将迭代器的每个元素添加到 ArrayBuffer中
        }
        val cameras = buffer.distinct //将相同卡扣Id下的摄像头 去重 得到所有有用的摄像头
        (x._1, cameras) // (卡扣ID,摄像头id的ArrayBuffer)
      })

    val monitor_camera_info = sc.textFile("/qftfmp/basic/monitor_camera_info")
      .map(x => {
        val arr = x.split("\001")
        val monitor_id = arr(0)
        val camera_id = arr(1)
        (monitor_id, camera_id)
      }).groupByKey()
      .map(x => {
        val itr = x._2.toIterator
        val cameras = new ArrayBuffer[String]()
        while (itr.hasNext) {
          val camera = itr.next()
          cameras += camera
        }
        (x._1, cameras) // (卡扣ID,摄像头id的ArrayBuffer)
      })

    //将以下变量封装为累加器
    val normalMonitorCount = sc.longAccumulator("normalMonitorCount") //正常卡扣个数
    val normalCameraCount = sc.longAccumulator("normalCameraCount") //正常的摄像头个数
    val abnormalMonitorCount = sc.longAccumulator("abnormalMonitorCount") //异常的卡扣个数
    val abnormalCameraCount = sc.longAccumulator("abnormalCameraCount") //异常的摄像头个数

    //进行左连接，获取指标数据
    monitor_camera_info.leftOuterJoin(monitor_flow_action)
      .map(x => {
        val buffer = new StringBuffer() //用来拼接坏的摄像头信息
        val monitorId = x._1
        val basicCameraIdArr = x._2._1 // 码表的摄像头id Array
        val factCameraIdArr = x._2._2.get //Option.get  事实表的摄像头id Array
        if (factCameraIdArr.length == basicCameraIdArr.length) { //判断有用的摄像头和所有的摄像头的个数是否一样
          normalMonitorCount.add(1) //相同卡扣下，若相同，则说明这个卡扣是正常的，累加器加1
        } else {
          abnormalMonitorCount.add(1) //不相同则说明卡扣是不正常的
          for (basicCameraId <- basicCameraIdArr) { //遍历码表中的所有摄像头
            if (!factCameraIdArr.contains(basicCameraId)) { //判断事实表中摄像头是否都包含了码表中的摄像头
              abnormalCameraCount.add(1)
              buffer.append("," + basicCameraId)
            } else { //如果事实表有这个摄像头，说明这个摄像头是可用的
              normalCameraCount.add(1)
            }
          }
        }
        //异常卡扣数量中哪些摄像头异常，需要保存摄像头的编号
        monitorId + "\001" + buffer.toString.substring(1)
      }).saveAsTextFile("/qftfmp/result/monitorAnalyze/abnormalCameraInfo")

    //将正常的，异常的卡扣和摄像头个数落地在指定路径
    sc.makeRDD(List[String](normalMonitorCount.value.toString + "\001" + normalCameraCount.value.toString + "\001"
      + abnormalMonitorCount.value.toString + "\001" + abnormalCameraCount.value.toString))
      .saveAsTextFile("/qftfmp/result/monitorAnalyze/monitorAndCameraStatus")
  }
}
