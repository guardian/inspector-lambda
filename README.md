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

To deploy Inspector Lambda, make sure you have **deployTools** credentials and run

```
./deploy-lambda.sh
```


inspector-lambda is then installed in target accounts via a stack set from the Guardian **Root** account.
