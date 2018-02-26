package com.gu.inspectorlambda

import java.lang.System

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.ec2.model.{CreateTagsRequest, Instance, Reservation, Tag}
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2Async, AmazonEC2AsyncClientBuilder, AmazonEC2ClientBuilder}
import com.amazonaws.services.inspector.model._
import com.amazonaws.services.inspector.{AmazonInspector, AmazonInspectorClient, AmazonInspectorClientBuilder}
import com.amazonaws.services.lambda.model.ListTagsRequest
import com.amazonaws.services.lambda.runtime.events.ConfigEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import scala.collection.JavaConverters._

class Lambda extends RequestHandler[ConfigEvent, Unit] with StrictLogging {

//  "name": "Security Best Practices",
//  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6",

//  "name": "Runtime Behavior Analysis",
//  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd",

//  "name": "CIS Operating System Security Configuration Benchmarks",
//  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-sJBhCr0F",

//  "name": "Common Vulnerabilities and Exposures",
//  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-ubA5XvBh",

  private val interestingTags = List("App", "Stack", "Stage")
  private val instancesPerTagCount = 5
  private val inspectionTagName = "Inspection"

  override def handleRequest(input: ConfigEvent, context: Context): Unit = {
    val ec2Client = com.amazonaws.services.ec2.AmazonEC2ClientBuilder.defaultClient()
    val inspectorClient = com.amazonaws.services.inspector.AmazonInspectorClientBuilder.defaultClient()
    doIt(ec2Client, inspectorClient)
  }

  def doIt(ec2Client: AmazonEC2, inspectorClient: AmazonInspector) = {
    val instances = getInstances(ec2Client)

    val matchingInstanceSets = (for {
      tagCombo <- getTagCombos(instances)
      newTag = constructNewTag(tagCombo)
      matchingInstances = getInstancesWithMatchingTags(instances, tagCombo)
    } yield (newTag -> matchingInstances)).toMap

    matchingInstanceSets.foreach(mis => {
      logger.info(s"Found set: ${mis._1} -> ${mis._2}")
      val tagsRequest = new CreateTagsRequest()
        .withTags(new Tag(inspectionTagName, mis._1))
        .withResources(mis._2.toList.asJava)
      ec2Client.createTags(tagsRequest)
      val createResourceGroupRequest = new CreateResourceGroupRequest()
          .withResourceGroupTags(new ResourceGroupTag()
            .withKey(inspectionTagName)
            .withValue(mis._1)
          )
      val createResourceGroupResult = inspectorClient.createResourceGroup(createResourceGroupRequest)
      val arn = createResourceGroupResult.getResourceGroupArn

      val createAssessmentTargetRequest = new CreateAssessmentTargetRequest()
        .withResourceGroupArn(arn)
        .withAssessmentTargetName(mis._1)
      val createAssessmentTargetResult = inspectorClient.createAssessmentTarget(createAssessmentTargetRequest)
      val findingsAttribute = new Attribute().withKey(inspectionTagName).withValue(mis._1)
      val createAssessmentTemplateRequest = new CreateAssessmentTemplateRequest()
        .withAssessmentTargetArn(createAssessmentTargetResult.getAssessmentTargetArn)
        .withDurationInSeconds(3600)
        .withRulesPackageArns(
          "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd",
          "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6"
        )
        .withUserAttributesForFindings(findingsAttribute)
        .withAssessmentTemplateName(mis._1)
      val createAssessmentTemplateResult = inspectorClient.createAssessmentTemplate(createAssessmentTemplateRequest)
      val startAssessmentRunRequest = new StartAssessmentRunRequest()
        .withAssessmentRunName(mis._1)
        .withAssessmentTemplateArn(createAssessmentTemplateResult.getAssessmentTemplateArn)
      try {
        inspectorClient.startAssessmentRun(startAssessmentRunRequest)
        logger.error(s"Assessment run started for ${mis._1}")
      } catch {
        case e: com.amazonaws.services.inspector.model.InvalidInputException =>
          logger.error(s"No instances available for ${mis._1}")
      }
    })

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
    val ec2Client =  AmazonEC2AsyncClientBuilder.standard()
      .withCredentials(new ProfileCredentialsProvider(args(0)))
      .withRegion(Regions.EU_WEST_1)
      .build()
    val inspectorClient =  AmazonInspectorClientBuilder.standard()
      .withCredentials(new ProfileCredentialsProvider(args(0)))
      .withRegion(Regions.EU_WEST_1)
      .build()
    new Lambda().doIt(ec2Client, inspectorClient)
  }
}