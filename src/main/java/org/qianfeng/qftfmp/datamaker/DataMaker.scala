package org.qianfeng.qftfmp.datamaker

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.util.Date


import org.apache.spark.sql.Row
import org.qianfeng.qftfmp.utils.{FullMapUtils, StringUtils}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.Random


object DataMaker {
  //存储表1的数据结构 创建可变list集合 ListBuffer
  val dataList=ListBuffer.newBuilder[Row].result()

//创建文件 数据表和字典表
  val dataFile="data/monitor_flow_action"
  val info="data/monitor_camera_info"
  val pri=new PrintWriter(new File(dataFile))
  val prin=new PrintWriter(new File(info))

  def main(args: Array[String]): Unit = {
     createFile(dataFile)
     createFile(info)
     print("start")
     makedata()
     createDataOfTable2
  }

  def makedata()= {
        val map=FullMapUtils.fullMap()
        //随机数
        val ran=Random.self

        //车牌打头的中文
        val location=List("京","豫", "鲁", "京", "京", "京", "沪", "京", "京", "深")
        //时间
        val currentDate=new SimpleDateFormat("yyyy-MM-dd").format(new Date())
        /*模拟3000车的数据
           创建24小时制的hour
           每辆车被拍30次加1小时 随机被拍数量，卡扣，摄像
            每次被拍 写入文件

         */
        for(i<-0 to 3000){
          println("kaishi")
          if(i==1000) println(1000)
          //车牌
          val car = location(ran.nextInt(10)) + (65 + ran.nextInt(26)).toChar + StringUtils.fulfuill(5, ran.nextInt(100000) + "")
          //带上小时的时间
          var withHourDate=currentDate+" "+StringUtils.fulfuill(ran.nextInt(24)+"")
          //随机一辆车经过的摄像 所属的卡扣
          for(j<-0 to ran.nextInt(300)+1){
            if (j % 30 == 0 && j != 0 && withHourDate.split(" ")(1).toInt!=23) withHourDate = currentDate + " " + StringUtils.fulfuill((withHourDate.split(" ")(1).toInt + 1) + "")

            val actionTime = withHourDate + ":" + StringUtils.fulfuill(ran.nextInt(60) + "") + ":" + StringUtils.fulfuill(ran.nextInt(60) + "") //模拟经过此卡扣开始时间 ，如：2017-10-01 20:09:10
            val id=ran.nextInt(9)
            val monitorId=StringUtils.fulfuill(4,id+"")
            val list:ListBuffer[Int]=map.getOrElse(id,ListBuffer.newBuilder[Int].result())
            val cid=list(ran.nextInt(list.size))
            val cameraId=StringUtils.fulfuill(5,cid+"")
            val areaId = StringUtils.fulfuill(2,cid%8+"")
            val speed = ran.nextInt(260) + 1 + "" //模拟速度
            val roadId = StringUtils.fulfuill(2,cid%50+"") //模拟道路id 【1~50 个道路】
            val content=currentDate+"\001"+monitorId+"\001"+cameraId+"\001"+car+"\001"+actionTime+"\001"+speed+"\001"+roadId+"\001"+areaId
            dataList += Row(currentDate,monitorId + "",cameraId,car,actionTime,speed,roadId,areaId)
           writeDataToFile(pri,content+System.lineSeparator())

          }
        }
        pri.flush()
        pri.close()

      }

      def createFile(path:String)={
        try {
          val file = new File(path)
          if (file.exists()) file.delete()
          file.createNewFile()
        }catch {
          case e:Exception=>print("创建失败")
        }
      }

      def writeDataToFile(pri:PrintWriter,content:String)={
        pri.append(content)
      }

    def createDataOfTable2 = {
      /**
        * monitorAndCameras   ： mutable.HashMap
        * key ：monitor_id
        * value : mutable.Set[String] (camera_id)
        * 基于生成的数据，生成对应的卡扣号和摄像头对应基本表
        */
      val monitorAndCameras = mutable.HashMap[String, mutable.Set[String]]()
      val datatableone = dataList

      //创建一个ArrayBuffer，用于存放本表的数据
      val dataList2 = ListBuffer.newBuilder[Row].result()

      //获取第一个表的所有数据
      val metaDataTable1 = datatableone

      var index = 0
      for (row: Row <- metaDataTable1) {
        val monitor_id: String = row.get(1).toString //该行数据中的monitor_id
        val camera_id: String = row.get(2).toString
        //该行数据中的camera_id
        val cameras: mutable.Set[String] = monitorAndCameras.getOrElse(monitor_id, mutable.Set[String]())

        //这里每隔1000次随机插入一条数据，模拟出来标准表中卡扣对应摄像头的数据比模拟数据中多出来的摄像头。
        // 这个摄像头的数据不一定会在车辆数据中有。即可以看出卡扣号下有坏的摄像头。
        index += 1
        if (index % 1000 == 0) {
          cameras.add(Random.self.nextInt(100000)+"")
        }
        //将monitor_id对应的camera_id存放入Set集合中
        cameras.add(camera_id)
        monitorAndCameras.put(monitor_id, cameras)
      }

      //获取monitor_id与其对应的所有camera_id数据
      for (ele <- monitorAndCameras) {
        val monitor_id: String = ele._1
        val cameras: mutable.Set[String] = ele._2
        println(cameras.size)
        //将获取到的monitor_id和cameras追加入List中，表示映射关系
        for (camera_id <- cameras) {
          val content = monitor_id + "\001" + camera_id
          writeDataToFile(prin, content+System.lineSeparator())
        }
      }

      prin.flush()
      prin.close()

    }

 }