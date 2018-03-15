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

### Local development and deployment to Deploy Tools

To deploy a new release of Inspector Lambda, you may first want to update the version value in **version.sbt**, then make sure you have **deployTools** credentials and run

```
./jar-upload-to-s3.sh
```

### Team release

The lambda is pushed to other AWS accounts using StackSets (see security team for details).

