#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SCALA_FOLDER="scala-2.12" 
SOFTWARE_VERSION="0.1.0" # Mirrors the value of softwareVersion in build.sbt
JARFILENAME="inspector-lambda-$SOFTWARE_VERSION.jar"

cd $DIR
sbt assembly

aws s3 cp "target/scala-2.12/$JARFILENAME" "s3://guardian-dist/guardian/PROD/inspector-lambda/$JARFILENAME" --profile deployTools

aws --profile security --region eu-west-1 \
    lambda update-function-code --function-name inspectorlambda \
    --s3-bucket guardian-dist \
    --s3-key "guardian/PROD/inspector-lambda/$JARFILENAME"

