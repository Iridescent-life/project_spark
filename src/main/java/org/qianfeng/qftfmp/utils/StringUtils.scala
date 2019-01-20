package org.qianfeng.qftfmp.utils

object StringUtils {
  /**
    * 当str的长度不满足num时，
    * 为传入的str补0，直至长度达到num的要求
    * @param num  需求的字符串长度
    * @param str  需要补0的字符串
    * @return 返回一个满足指定长度的字符串
    */
  def fulfuill(num: Int, str: String): String = {
    if (num == str.length) {
      str;
    } else {
      val i = num - str.length
      var tmp = ""
      for (t <- 0 to i - 1) {
        tmp = tmp.concat("0")
      }
      tmp.concat(str)
    }
  }

  /**
    * 当传入的 时分秒 位数小于2时，为其进行补0操作
    * @param str 表示 时，分，秒的字符串
    * @return 返回补0后的字符串
    */
  def fulfuill(str: String): String = {
    if (str.length == 1) return "0" + str
    str
  }

  /**
    * 截断字符串两侧的逗号
    * @param str 字符串
    * @return 截断后的字符串
    */
  def trimComma(str: String): String = {
    var newStr = ""
    if (str.startsWith(",")) newStr = str.substring(1)
    if (str.endsWith(",")) newStr = newStr.substring(0, str.length - 1)
    newStr
  }
}
