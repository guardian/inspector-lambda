package com.gu.inspectorlambda

import java.lang.System

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.ec2.model.{Instance, Reservation}
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2Async, AmazonEC2AsyncClientBuilder, AmazonEC2ClientBuilder}
import com.amazonaws.services.lambda.model.ListTagsRequest
import com.amazonaws.services.lambda.runtime.events.ConfigEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.collection.JavaConverters._

class Lambda extends RequestHandler[ConfigEvent, Unit] with StrictLogging {

  private val interestingTags = List("App", "Stack", "Stage")
  private val instancesPerTagCount = 5

  override def handleRequest(input: ConfigEvent, context: Context): Unit = {
    val client = com.amazonaws.services.ec2.AmazonEC2ClientBuilder.defaultClient()
    doIt(client)
  }

  def doIt(client: AmazonEC2) = {
    val instances = getInstances(client)
    val matchingInstanceSets = for {
      tagCombo <- getTagCombos(instances)
      matchingInstances = getInstancesWithMatchingTags(instances, tagCombo)
    } yield matchingInstances
    matchingInstanceSets.foreach(mis => logger.info(s"Found set of $mis"))
    logger.info("Done")
  }

  private def constructNewTag(tagCombo: TagCombo): String = {
    val epoch = System.currentTimeMillis / 1000
    val stack = tagCombo.stack.getOrElse("None")
    val app = tagCombo.app.getOrElse("None")
    val stage = tagCombo.stage.getOrElse("None")
    s"$stack-$app-$stage-$epoch"
  }


  private def getInstances(client: AmazonEC2): Set[Instance] = {
    (for {
      reservation <- client.describeInstances().getReservations.asScala
      instance <- reservation.getInstances.asScala
      if instance.getState.getName.equals("running")
    } yield instance).toSet
  }

  private def getInstancesWithMatchingTags(instances: Set[Instance], tc:TagCombo): Set[String] = {
    val instancesWithApp = getInstancesWithMatchingTag(instances, "App", tc.app)
    val instancesWithStack = getInstancesWithMatchingTag(instancesWithApp, "Stack", tc.stack)
    val instancesWithStage = getInstancesWithMatchingTag(instancesWithStack, "Stage", tc.stage)
    instancesWithStage.take(instancesPerTagCount).map(i => i.getInstanceId)
  }
  
  private def getInstancesWithMatchingTag(instances: Set[Instance], key:String, value:Option[String]): Set[Instance] = {
    value match {
      case None => {
        for {
          instance <- instances
          if instance.getTags.asScala.filter(t => t.getKey.equals(key)).isEmpty
        } yield instance
      }
      case Some(realValue) => {
        for {
          instance <- instances
          tag <- instance.getTags.asScala.filter(t => t.getKey.equals(key))
          if tag.getValue.equals(realValue)
        } yield instance
      }
    }
  }

  private def getTagCombos(instances: Set[Instance]) = {
    for {
      instance <- instances
      tags = instance
        .getTags
        .asScala
        .map(t => (t.getKey, t.getValue)).toMap
        .filter(p => interestingTags.filter(q => p._1.equals(q)).nonEmpty)
      app = tags.get("App")
      stack = tags.get("Stack")
      stage = tags.get("Stage")
    } yield TagCombo(app, stack, stage)
  }
}

case class TagCombo(app:Option[String], stack:Option[String], stage:Option[String])


object Lambda {
  def main(args: Array[String]): Unit = {
    if (args.size != 1) {
      println("Must provide a credentials stanza name")
      System.exit(1)
    }
    val client =  AmazonEC2AsyncClientBuilder.standard()
      .withCredentials(new ProfileCredentialsProvider(args(0)))
      .withRegion(Regions.EU_WEST_1)
      .build()
    new Lambda().doIt(client)
  }
}