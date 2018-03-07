#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SCALA_FOLDER="scala-2.12" 
SOFTWARE_VERSION="0.1.0" # Mirrors the value of softwareVersion in build.sbt
JARFILENAME="inspector-lambda-$SOFTWARE_VERSION.jar"

cd "$DIR/.."
sbt assembly
aws s3 cp "target/$SCALA_FOLDER/$JARFILENAME" "s3://guardian-dist/guardian/PROD/inspector-lambda/$JARFILENAME" --profile deployTools

