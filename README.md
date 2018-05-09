When one works in an enterprise it would be super useful if it was
possible to package a node app inside a maven wrapper.

This should be very easy. Java has the concept of an uberjar, a single
jar where everything is packaged up (incuding non-class files) and is
executable.

So this is an attempt to make a library that can then be used in maven
to package a node app.

The executable program in this library will be used to extract and
execute the node app.
