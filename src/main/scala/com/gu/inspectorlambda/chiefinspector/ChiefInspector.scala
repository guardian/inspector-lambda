package com.gu.inspectorlambda.chiefinspector

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.{CreateTagsRequest, Tag}
import com.amazonaws.services.inspector.AmazonInspector
import com.amazonaws.services.inspector.model._
import com.gu.inspectorlambda.model.{SimpleInstance, TagCombo}

import scala.collection.JavaConverters._

object ChiefInspector extends StrictLogging {

  //  "name": "Security Best Practices",
  //  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6",

  //  "name": "Runtime Behavior Analysis",
  //  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd",

  //  "name": "CIS Operating System Security Configuration Benchmarks",
  //  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-sJBhCr0F",

  //  "name": "Common Vulnerabilities and Exposures",
  //  "arn": "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-ubA5XvBh",

  private val instancesPerTagCount = 5
  private val inspectionTagName = "Inspection"

  def createAndRunAssessments(ec2Client: AmazonEC2, inspectorClient: AmazonInspector): Unit = {
    val instances = getRunningInstances(ec2Client)

    val matchingInstanceSets = (for {
      tagCombo <- getTagCombos(instances)
      newTag = constructNewTag(tagCombo)
      matchingInstances = getInstancesWithMatchingTags(instances, tagCombo)
    } yield newTag -> matchingInstances).toMap

    matchingInstanceSets.foreach( mis => {
      val name = mis._1
      val instanceIds = mis._2
      logger.info(s"Found set: $name -> $instanceIds")
      createTags(ec2Client, name, instanceIds)
      val resourceGroupArn: String = createResourceGroup(inspectorClient, name)
      val assessmentTargetArn = createAssessmentTarget(inspectorClient, name, resourceGroupArn)
      val assessmentTemplateArn = createAssessmentTemplate(inspectorClient, name, assessmentTargetArn)
      startAssessmentRun(inspectorClient, name, assessmentTemplateArn)
    })

    logger.info("Done")
  }

  private def startAssessmentRun(inspectorClient: AmazonInspector, name: String, assessmentTemplateArn: String): Unit = {
    val startAssessmentRunRequest = new StartAssessmentRunRequest()
      .withAssessmentRunName(name)
      .withAssessmentTemplateArn(assessmentTemplateArn)
    try {
      inspectorClient.startAssessmentRun(startAssessmentRunRequest)
      logger.error(s"Assessment run started for $name")
    } catch {
      case e: InvalidInputException =>
        logger.error(s"No instances available for $name (${e.getMessage})")
    }
  }

  private def createAssessmentTemplate(inspectorClient: AmazonInspector, name: String, arn: String) = {
    val createAssessmentTemplateRequest = new CreateAssessmentTemplateRequest()
      .withAssessmentTargetArn(arn)
      .withDurationInSeconds(3600)
      .withRulesPackageArns(
        "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd",
        "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6"
      )
      .withUserAttributesForFindings(new Attribute().withKey(inspectionTagName).withValue(name))
      .withAssessmentTemplateName(name)
    val createAssessmentTemplateResult = inspectorClient.createAssessmentTemplate(createAssessmentTemplateRequest)
    createAssessmentTemplateResult.getAssessmentTemplateArn
  }

  private def createAssessmentTarget(inspectorClient: AmazonInspector, name: String, arn: String) = {
    val createAssessmentTargetRequest = new CreateAssessmentTargetRequest()
      .withResourceGroupArn(arn)
      .withAssessmentTargetName(name)
    val createAssessmentTargetResult = inspectorClient.createAssessmentTarget(createAssessmentTargetRequest)
    createAssessmentTargetResult.getAssessmentTargetArn
  }

  private def createTags(ec2Client: AmazonEC2, name: String, instanceIds: Set[String]) = {
    val tagsRequest = new CreateTagsRequest()
      .withTags(new Tag(inspectionTagName, name))
      .withResources(instanceIds.toList.asJava)
    ec2Client.createTags(tagsRequest)
  }

  private def createResourceGroup(inspectorClient: AmazonInspector, name: String) = {
    val createResourceGroupRequest = new CreateResourceGroupRequest()
      .withResourceGroupTags(new ResourceGroupTag()
        .withKey(inspectionTagName)
        .withValue(name)
      )
    val createResourceGroupResult = inspectorClient.createResourceGroup(createResourceGroupRequest)
    createResourceGroupResult.getResourceGroupArn
  }

  private[inspectorlambda] def constructNewTag(tagCombo: TagCombo): String = {
    val epoch = System.currentTimeMillis / 1000
    val stack = tagCombo.stack.getOrElse("None")
    val app = tagCombo.app.getOrElse("None")
    val stage = tagCombo.stage.getOrElse("None")
    s"$stack-$app-$stage-$epoch"
  }

  private def getRunningInstances(client: AmazonEC2): Set[SimpleInstance] = {
    (for {
      reservation <- client.describeInstances().getReservations.asScala
      instance <- reservation.getInstances.asScala
      if instance.getState.getName.equals("running")
      tags = instance.getTags.asScala.map(t => t.getKey -> t.getValue).toMap
      simpleInstance = SimpleInstance(instance.getInstanceId, tags)
    } yield simpleInstance).toSet
  }

  private[inspectorlambda] def getInstancesWithMatchingTags(instances: Set[SimpleInstance], tc:TagCombo): Set[String] = {
    val instancesWithApp = getInstancesWithMatchingTag(instances, "App", tc.app)
    val instancesWithStack = getInstancesWithMatchingTag(instancesWithApp, "Stack", tc.stack)
    val instancesWithStage = getInstancesWithMatchingTag(instancesWithStack, "Stage", tc.stage)
    instancesWithStage.take(instancesPerTagCount).map(i => i.instanceId)
  }

  private[inspectorlambda] def getInstancesWithMatchingTag(instances: Set[SimpleInstance], key:String, value:Option[String]): Set[SimpleInstance] = {
    value match {
      case None =>
        for {
          instance <- instances
          if !instance.tags.keys.toSet.contains(key)
        } yield instance
      case Some(realValue) =>
        for {
          instance <- instances
          tag <- instance.tags.toSet.filter(t => t._1.equals(key))
          if tag._2.equals(realValue)
        } yield instance
    }
  }

  private def getTagCombos(instances: Set[SimpleInstance]) = {
    for {
      instance <- instances
      app = instance.tags.get("App")
      stack = instance.tags.get("Stack")
      stage = instance.tags.get("Stage")
    } yield TagCombo(stack, app, stage)
  }
}