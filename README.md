# inspector-lambda - ARCHIVED, superceded by other tools.
A lambda to:

* Find all combinations of the App, Stack, Stage tags (including `not present`)
* Find the youngest `n` running instances of each combination
* Add a unique tag to each of the instances
* Schedule an AWS Inspector run against that tag

## Running locally

To run the lambda locally make sure you have Janus credentials for the corresponding account and run 

```
sbt ';run <account name>'
```

For instance 

```
sbt ';run security'
```

for the **security** account.

## Deployment

To deploy a new release of Inspector Lambda: 

- Update the version value in **version.sbt** as well as in the cloudformation file

- Make sure you have **deployTools** and **Root** credentials.

- Run (from the `script` folder)

	```
	./jar-upload-to-s3.sh
	```

- Apply the cloud-formation change from the Root account (StackSets deployment to other AWS accounts).

