package com.gu.inspectorlambda.chiefinspector

import com.typesafe.scalalogging.StrictLogging
import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.inspector.AmazonInspector
import com.gu.inspectorlambda.model.{SimpleInstance, TagCombo}
import com.gu.inspectorlambda.aws._

object ChiefInspector extends StrictLogging {

  val inspectionTagName = "Inspection"
  private val instancesPerTagCount = 5

  def createAndRunAssessments(ec2Client: AmazonEC2, inspectorClient: AmazonInspector): Unit = {
    val ec2 = new AWSEC2(ec2Client)
    val inspector = new AWSInspector(inspectorClient)

    inspector.registerRoleArn()

    val instances = ec2.getRunningInstances
    val allInstanceIds = instances.map(i => i.instanceId)

    val matchingInstanceSets = (for {
      tagCombo <- getTagCombos(instances)
      matchingInstances = getInstancesWithMatchingTags(instances, tagCombo)
    } yield tagCombo -> matchingInstances).toMap

    matchingInstanceSets.foreach { case (tagCombo, matchingInstanceIds) =>
      val name = constructName(tagCombo)
      logger.info(s"Found set: $name -> $matchingInstanceIds")
      ec2.removeTags(name, allInstanceIds)
      ec2.createTags(name, matchingInstanceIds)
    }

    // Sleeping for 10 seconds to allow for tags propagation
    Thread.sleep(10000)

    matchingInstanceSets.foreach { case (tagCombo, _) =>
      val name = constructName(tagCombo)
      val nameEpoch = constructNameEpoch(tagCombo)
      val resourceGroupArn: String = inspector.getResourceGroup(name) getOrElse inspector.createResourceGroup(name)
      val assessmentTargetArn = inspector.getAssessmentTarget(name, resourceGroupArn) getOrElse inspector.createAssessmentTarget(name, resourceGroupArn)
      val assessmentTemplateArn = inspector.getAssessmentTemplate(name, assessmentTargetArn) getOrElse inspector.createAssessmentTemplate(name, assessmentTargetArn)
      inspector.startAssessmentRun(nameEpoch, assessmentTemplateArn)
    }
  }

  private[inspectorlambda] def constructNameEpoch(tagCombo: TagCombo): String = {
    s"${constructName(tagCombo)}-${System.currentTimeMillis()}"
  }

  private[inspectorlambda] def constructName(tagCombo: TagCombo): String = {
    val stack = tagCombo.stack.getOrElse("None")
    val app = tagCombo.app.getOrElse("None")
    val stage = tagCombo.stage.getOrElse("None")
    s"AWSInspection-$stack-$app-$stage"
  }

  private[inspectorlambda] def getInstancesWithMatchingTags(instances: Set[SimpleInstance], tc:TagCombo): Set[String] = {
    val instancesWithApp = getInstancesWithMatchingTag(instances, "App", tc.app)
    val instancesWithStack = getInstancesWithMatchingTag(instancesWithApp, "Stack", tc.stack)
    val instancesWithStage = getInstancesWithMatchingTag(instancesWithStack, "Stage", tc.stage)
    instancesWithStage.take(instancesPerTagCount).map(i => i.instanceId)
  }

  private[inspectorlambda] def getInstancesWithMatchingTag(instances: Set[SimpleInstance], key:String, value:Option[String]): Set[SimpleInstance] = {
    value match {
      case None =>  instances.filter(i => !i.tags.keys.toSet.contains(key))
      case Some(realValue) => instances.filter(i => i.tags.toSet.contains((key, realValue)))
    }
  }

  private[inspectorlambda] def getTagCombos(instances: Set[SimpleInstance]) = {
    for {
      instance <- instances
      app = instance.tags.get("App")
      stack = instance.tags.get("Stack")
      stage = instance.tags.get("Stage")
    } yield TagCombo(stack, app, stage)
  }
}
