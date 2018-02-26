package com.gu.inspectorlambda

import com.amazonaws.services.lambda.runtime.events.ConfigEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.gu.inspectorlambda.chiefinspector.ChiefInspector

class Lambda extends RequestHandler[ConfigEvent, Unit] {

  override def handleRequest(input: ConfigEvent, context: Context): Unit = {
    val ec2Client = com.amazonaws.services.ec2.AmazonEC2ClientBuilder.defaultClient()
    val inspectorClient = com.amazonaws.services.inspector.AmazonInspectorClientBuilder.defaultClient()
    ChiefInspector.createAndRunAssessments(ec2Client, inspectorClient)
  }

}




