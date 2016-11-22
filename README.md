# slug

Slug is a microservice architecture generator, primarily used for generating architecutres that enable fast simulation for Lineage Driven Fault Injection.

# Build Status

[![CircleCI](https://circleci.com/gh/ashutoshraina/slug/tree/master.svg?style=svg)](https://circleci.com/gh/ashutoshraina/slug/tree/master)

How to run

```
clone the repo
git clone git@github.com:ashutoshraina/slug.git

```

```
./gradlew clean built test run
```

Output

In the samples directory : 

* There will be png images of the generated architecture as force-directed graphs.
* There will dot files which can be used to render the architectures in grpahviz.
* If you have installed graphviz, then you can do 

```
dot -Tpng samples/input.dot > output.png
``` 
