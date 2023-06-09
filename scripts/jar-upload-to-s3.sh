#!/bin/bash

cd "$( dirname "${BASH_SOURCE[0]}" )/.."

SCALA_FOLDER="scala-2.12" 
SOFTWARE_VERSION="$(date +%s)"
JARFILENAME="inspector-lambda"

sbt assembly
aws s3 cp "target/$SCALA_FOLDER/$JARFILENAME.jar" "s3://guardian-dist/guardian/PROD/inspector-lambda/$JARFILENAME-$SOFTWARE_VERSION.jar" --profile deployTools

