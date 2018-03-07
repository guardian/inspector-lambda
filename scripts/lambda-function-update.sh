#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
SOFTWARE_VERSION="0.1.0" # Mirrors the value of softwareVersion in build.sbt
JARFILENAME="inspector-lambda-$SOFTWARE_VERSION.jar"

if [ $# -eq 0 ]; then
    echo "No arguments provided."
    echo "usage: ./lambda-function-update.sh <aws-account-name>"
    exit 1
fi

AWSPROFILE="$1"

cd "$DIR/.."

aws --profile "$AWSPROFILE" --region eu-west-1 \
    lambda update-function-code --function-name inspectorlambda \
    --s3-bucket guardian-dist \
    --s3-key "guardian/PROD/inspector-lambda/$JARFILENAME"

