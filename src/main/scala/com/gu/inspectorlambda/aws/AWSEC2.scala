package com.gu.inspectorlambda.aws

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.{CreateTagsRequest, DeleteTagsRequest, Tag}
import com.gu.inspectorlambda.chiefinspector.ChiefInspector.inspectionTagName
import com.gu.inspectorlambda.model.SimpleInstance
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

class AWSEC2 (val client: AmazonEC2) extends StrictLogging {

  def getRunningInstances: Set[SimpleInstance] = {
    (for {
      reservation <- client.describeInstances().getReservations.asScala
      instance <- reservation.getInstances.asScala
      if instance.getState.getName.equals("running")
      tags = instance.getTags.asScala.map(t => t.getKey -> t.getValue).toMap
      simpleInstance = SimpleInstance(instance.getInstanceId, tags)
    } yield simpleInstance).toSet
  }

  def createTags(name: String, instanceIds: Set[String]): Unit = {
    val tagsRequest = new CreateTagsRequest()
      .withTags(new Tag(inspectionTagName, name))
      .withResources(instanceIds.toList.asJava)
    client.createTags(tagsRequest)
  }

  def removeTags(name: String, instanceIds: Set[String]): Unit = {
    val tagsRequest = new DeleteTagsRequest()
      .withTags(new Tag(inspectionTagName, name))
      .withResources(instanceIds.toList.asJava)
    client.deleteTags(tagsRequest)
    Thread.sleep(3)
  }

}