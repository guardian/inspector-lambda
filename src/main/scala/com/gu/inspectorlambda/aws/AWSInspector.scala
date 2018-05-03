package com.gu.inspectorlambda.aws

import com.amazonaws.services.inspector.AmazonInspector
import com.amazonaws.services.inspector.model._
import com.gu.inspectorlambda.chiefinspector.ChiefInspector.inspectionTagName
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

class AWSInspector(val client: AmazonInspector) extends StrictLogging {

  val sleepForConsistencyMillis = 2000

  def getResourceGroup(name: String): Option[String] = {

    val assessmentTargetArns = client.listAssessmentTargets(new ListAssessmentTargetsRequest()).getAssessmentTargetArns

    if (assessmentTargetArns.isEmpty)
      None
    else
      client.describeAssessmentTargets(new DescribeAssessmentTargetsRequest().withAssessmentTargetArns(assessmentTargetArns)).getAssessmentTargets.asScala
        .flatMap(assessmentTarget => {
          val describeResourceGroupsRequest = new DescribeResourceGroupsRequest().withResourceGroupArns(assessmentTarget.getResourceGroupArn)
          client.describeResourceGroups(describeResourceGroupsRequest).getResourceGroups.asScala
            .filter(resourceGroup => {
              resourceGroup.getTags.asScala
                .exists(p => p.getKey.equals(inspectionTagName) && p.getValue.equals(name))
            })
            .map(resourceGroup => resourceGroup.getArn)
        })
        .headOption
  }

  def createResourceGroup(name: String): String = {
    val createResourceGroupRequest = new CreateResourceGroupRequest()
      .withResourceGroupTags(new ResourceGroupTag()
        .withKey(inspectionTagName)
        .withValue(name)
      )
    val createResourceGroupResult = client.createResourceGroup(createResourceGroupRequest)
    createResourceGroupResult.getResourceGroupArn
  }

  def getAllAssessmentTargets(token: Option[String]): List[AssessmentTarget] = {

    val assessmentTargetsRequest = token match {
      case Some(s) => new ListAssessmentTargetsRequest().withNextToken(s)
      case None => new ListAssessmentTargetsRequest()
    }

    val assessmentTargets = client.listAssessmentTargets(assessmentTargetsRequest)
    val arns = assessmentTargets.getAssessmentTargetArns
    if (arns.isEmpty)
      List()
    else {
      val assessmentTargetDescriptions = client.describeAssessmentTargets(new DescribeAssessmentTargetsRequest().withAssessmentTargetArns(arns)).getAssessmentTargets.asScala.toList

      assessmentTargets.getNextToken match {
        case s: String => assessmentTargetDescriptions ::: getAllAssessmentTargets(Some(s))
        case _ => assessmentTargetDescriptions
      }
    }
  }

  def getAssessmentTarget(name: String, arn: String): Option[String] = {
    val assessmentTargets = getAllAssessmentTargets(None)

    val matchingAssessmentTargets = assessmentTargets.filter(assessmentTarget => assessmentTarget.getName.equals(name))

    // delete if arn is not correct
    matchingAssessmentTargets
      .filterNot{at: AssessmentTarget => at.getResourceGroupArn.equals(arn) }
      .foreach(assessmentTarget => {
        logger.info(s"Deleting ${assessmentTarget.getArn}")
        val deleteAssessmentTargetRequest = new DeleteAssessmentTargetRequest()
          .withAssessmentTargetArn(assessmentTarget.getArn)
        client.deleteAssessmentTarget(deleteAssessmentTargetRequest)
        Thread.sleep(sleepForConsistencyMillis)
      })

    // Return if arn is correct
    matchingAssessmentTargets
      .filter{at: AssessmentTarget => at.getResourceGroupArn.equals(arn) }
      .map(_.getArn)
      .headOption
  }

  def createAssessmentTarget(name: String, arn: String): String = {
    val createAssessmentTargetRequest = new CreateAssessmentTargetRequest()
      .withResourceGroupArn(arn)
      .withAssessmentTargetName(name)
    val createAssessmentTargetResult = client.createAssessmentTarget(createAssessmentTargetRequest)
    createAssessmentTargetResult.getAssessmentTargetArn
  }

  def getAssessmentTemplate(name: String, arn: String): Option[String] = {

    val assessmentTemplateArns = client.listAssessmentTemplates(new ListAssessmentTemplatesRequest()).getAssessmentTemplateArns
    if (assessmentTemplateArns.isEmpty)
      None
    else {

      val matchingAssessmentTemplates = client.describeAssessmentTemplates(new DescribeAssessmentTemplatesRequest().withAssessmentTemplateArns(assessmentTemplateArns)).getAssessmentTemplates.asScala
        .filter(assessmentTemplate => assessmentTemplate.getName.equals(name))

      // delete if arn is not correct
      matchingAssessmentTemplates
        .filterNot(assessmentTemplate => assessmentTemplate.getAssessmentTargetArn.equals(arn))
        .foreach(assessmentTemplate => {
          val deleteAssessmentTemplateRequest = new DeleteAssessmentTemplateRequest()
            .withAssessmentTemplateArn(assessmentTemplate.getArn)
          client.deleteAssessmentTemplate(deleteAssessmentTemplateRequest)
          Thread.sleep(sleepForConsistencyMillis)
        })

      // Return if arn is correct
      matchingAssessmentTemplates
        .filter(assessmentTemplate => assessmentTemplate.getAssessmentTargetArn.equals(arn))
        .map(assessmentTemplate => assessmentTemplate.getArn)
        .headOption
    }
  }

  private val SECURITY_BEST_PRACTICES
    = "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-SnojL3Z6"
  //noinspection ScalaUnusedSymbol
  private val CIS_OPERATING_SYSTEM_SECURITY_CONFIGURATION_BENCHMARKS
    = "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-sJBhCr0F"
  private val RUNTIME_BEHAVIOUR_ANALYSIS
    = "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-lLmwe1zd"
  //noinspection ScalaUnusedSymbol
  private val COMMON_VULNERABILITIES_AND_EXPOSURES
    = "arn:aws:inspector:eu-west-1:357557129151:rulespackage/0-ubA5XvBh"

  def createAssessmentTemplate(name: String, arn: String): String = {
    val createAssessmentTemplateRequest = new CreateAssessmentTemplateRequest()
      .withAssessmentTargetArn(arn)
      .withDurationInSeconds(3600)
      .withRulesPackageArns(
        SECURITY_BEST_PRACTICES,
        RUNTIME_BEHAVIOUR_ANALYSIS
      )
      .withUserAttributesForFindings(new Attribute().withKey(inspectionTagName).withValue(name))
      .withAssessmentTemplateName(name)
    val createAssessmentTemplateResult = client.createAssessmentTemplate(createAssessmentTemplateRequest)
    createAssessmentTemplateResult.getAssessmentTemplateArn
  }

  def startAssessmentRun(name: String, assessmentTemplateArn: String): Unit = {
    val startAssessmentRunRequest = new StartAssessmentRunRequest()
      .withAssessmentRunName(name)
      .withAssessmentTemplateArn(assessmentTemplateArn)
    try {
      client.startAssessmentRun(startAssessmentRunRequest)
      logger.info(s"Assessment run started for $name")
    } catch {
      case e: InvalidInputException =>
        logger.error(s"Unable to start Assessment run '$name' (${e.getMessage})")
    }
  }

}