package org.qianfeng.qftfmp.utils

import java.text.SimpleDateFormat
import java.util.Date

object DateUtils {
  //返回当天的时间
  def getTodayDate = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
  }
}
