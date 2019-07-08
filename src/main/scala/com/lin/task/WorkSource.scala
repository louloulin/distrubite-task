package com.lin.task

/**
  *
  * @param id 事件id
  * @param eventName 事件名称
  * @param data 事件数据
  */
case class WorkSource(id:String, eventName:String, data:Array[Byte])
