// IPrinterCallback.aidl
package com.xcheng.printerservice;

// Declare any non-default types here with import statements

interface IPrinterCallback {
  /**
   * 执行发生异常
   * code： 异常代码
   * msg:	 异常描述
   */
  oneway void  onException(int code, String msg);

  /**
   * 打印机打印长度
   * current： 当前单词打印长度
   * total:	 开机之后打印总长度
   */
  oneway void  onLength(long current, long total);

  /**
   * 打印机打印长度
   * realCurrent： 当前单词打印长度(double型，不四舍五入)
   * realTotal:    开机之后打印总长度(double型，不四舍五入)
   */
  oneway void  onRealLength(double realCurrent, double realTotal);

  oneway void onComplete();
}
