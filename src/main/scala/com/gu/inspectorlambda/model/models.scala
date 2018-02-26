package com.gu.inspectorlambda.model

case class TagCombo(stack:Option[String], app:Option[String], stage:Option[String])

case class SimpleInstance(instanceId: String, tags:Map[String, String])