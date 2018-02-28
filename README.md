# inspector-lambda

A lambda to:
  * Find all combinations of the App, Stack, Stage tags (including 'not present')
  * Find the youngest n running instances with each combination
  * Add a unique tag to each of the instances
  * Schedule an AWS Inspector run against that tag

This application is built with `sbt assembly` and should be published to
`s3://guardian-dist/guardian/PROD/inspectory-lambda.jar`.

This application is installed in target accounts via a stack set from the Guardian root account.
