# inspector-lambda

A lambda to:

* Find all combinations of the App, Stack, Stage tags (including `not present`)
* Find the youngest `n` running instances of each combination
* Add a unique tag to each of the instances
* Schedule an AWS Inspector run against that tag

## Running locally

To run the lambda locally make sure you have Janus credentials for the corresponsing account and run 

```
sbt ';run <account name>'
```

For instance 

```
sbt ';run security'
```

for the **security** account.

## Deployment

This application is built with `sbt assembly` and should be published to

```
s3://guardian-dist/guardian/PROD/inspector-lambda/inspector-lambda.jar
``` 

in the **Deploy Tools** account.

You can perform this operation at the command line while updating the lamdba itself with

```
aws --profile security --region eu-west-1 \
    lambda update-function-code --function-name inspectorlambda \
    --s3-bucket guardian-dist \
    --s3-key guardian/PROD/inspector-lambda/inspector-lambda.jar
```

inspector-lambda is then installed in target accounts via a stack set from the Guardian **Root** account.
