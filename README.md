
KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
KAnnotator



#CREATE PULL REQUEST

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
KAnnotator



#CREATE PULL REQUEST

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format#R




#TEST CLAS


KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator

#TEST CLA


KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

KAnnotator
==========

Infer @Nullable/@NotNull and other annotations from byte code

### What is KAnnotator

If you have a Java library binaries (e.g. jar-files), KAnnotator can analyze them and tell you some information about contracts of the methods in this library. For example:
* Does this method admit `null` as a value for this parameter?
* Does this method ever return `null`?
* Does this method mutate this collection passed to it?

### Output format

The contracts are represented as annotations on Java method, hence the name of the project.
Since libraries under analysis are not under user's control, the annotations can not be added to their code directly, so KAnnotator stores them in one of the two formats:

#TEST CLA
* XML-based External annotation definitions supported by IntelliJ IDEA
* ````.jaif````-files, supported by [Checkers Framework](http://types.cs.washington.edu/annotation-file-utilities/)

### Kotlin and KAnnotator

KAnnotator was originally created to support [Kotlin](http://kotlinlang.org) — a JVM-targeted programming language that has null-safety built into its type system. Our experience has shown that external annotations are not a good fit for the Kotlin workflow, so we've removed support for external annotations from Kotlin and added support for [platform types](https://kotlinlang.org/docs/reference/java-interop.html#null-safety-and-platform-types) instead.

**If you're using Kotlin, you do not need KAnnotator**

### Java 8 and KAnnotator

With [JSR 308 and Checkers Framework](http://types.cs.washington.edu/jsr308/) coming soon, KAnnotator gets another application area: the annotations it infers for Java libraries can be used by Java code checkers to verify Java code that uses these libraries.

### Downloads

KAnnotator binaries are available from the [github release page](https://github.com/JetBrains/kannotator/releases)

Additionally, IntelliJ IDEA plugin is available though a [plugin repository](http://www.jetbrains.com/idea/plugins/)

### How to run KAnnotator

KAnnotator can be run by different means:
* [IntelliJ IDEA plug-in](http://plugins.jetbrains.com/plugin/7205?pr=idea): install and follow the instruction from [here](http://blog.jetbrains.com/kotlin/2013/03/kannotator-0-1-is-out/).
* Command line

````
$ java -jar kannotator-cli.jar
  -f --format=<enum>                - output format: jaif or xml [default XML]
  -n --nullability=<boolean>        - produce nullability annotations [default false]
  -m --mutability=<boolean>         - produce mutability annotations [default false]
  -v --verbose=<boolean>            - be verbose and show progress indicator [default false]
  -c --one-directory-tree=<boolean> - do not create specific subdirectories for each library [default false]
  -o --output-path=<string>         - output path [default annotations/]
````
