#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ $# -eq 0 ]; then
    echo "No arguments provided."
    echo "usage: ./lambda-function-update.sh <aws-account-name>"
    exit 1
fi

AWSPROFILE="$1"

cd "$DIR/.."

SOFTWARE_VERSION=`cut -d '"' -f 2 version.sbt `
JARFILENAME="inspector-lambda-$SOFTWARE_VERSION.jar"

aws --profile "$AWSPROFILE" --region eu-west-1 \
    lambda update-function-code --function-name inspectorlambda \
    --s3-bucket guardian-dist \
    --s3-key "guardian/PROD/inspector-lambda/$JARFILENAME"

