#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd "$DIR/.."

SCALA_FOLDER="scala-2.12" 
SOFTWARE_VERSION=`cut -d '"' -f 2 version.sbt `
JARFILENAME="inspector-lambda-$SOFTWARE_VERSION.jar"

sbt assembly
aws s3 cp "target/$SCALA_FOLDER/$JARFILENAME" "s3://guardian-dist/guardian/PROD/inspector-lambda/$JARFILENAME" --profile deployTools

