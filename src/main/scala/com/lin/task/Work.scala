package com.lin.task

case class Work(workId: String, job: Any)

case class WorkResult(workId: String, result: Any)

case class Tick(workSource: WorkSource)
