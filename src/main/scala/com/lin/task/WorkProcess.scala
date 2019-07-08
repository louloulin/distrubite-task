package com.lin.task

case class WorkRes(string:String)

case class WorkProcess() {
   def process(workSource: WorkSource): WorkRes ={
      return WorkRes(workSource.toString)
   }
}
