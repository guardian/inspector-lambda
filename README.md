# inspector-lambda

A lambda to:
  Find all combinations of the App, Stack, Stage tags (including 'not present')
  Find the youngest n running instances with each combination
  Add a unique tag to each of the instances
  Schedule an AWS Inspector run against that tag
