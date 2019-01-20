package org.qianfeng.qftfmp.utils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random

object FullMapUtils {
  //此方法将摄像头编号随机分配给不同的卡扣，保证不同卡扣中没有相同的摄像头
  def fullMap(): mutable.HashMap[Int, ListBuffer[Int]] = {
    val map = new mutable.HashMap[Int, ListBuffer[Int]]()
    for (i <- 0 to 8) { //编号作为key
      map.put(i, ListBuffer.newBuilder[Int].result())
    }

    val ran = Random.self
    for (i <- 0 to 99999) { //摄像编号
      val key = ran.nextInt(9) //卡扣编号
      map.getOrElse(key, ListBuffer.newBuilder[Int].result()).append(i)
    }
    map
  }
}
