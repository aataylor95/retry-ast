# **\@Retry** - retry-ast

Catches exceptions and employs retry logic. Useful for things with an expected fault tolerance like S3 calls or other network traffic.

Works by injecting an overloaded method (wrapped in try/catch) with a retry count parameter and also wrapping exisiting method in a try/catch which can call the overloaded method N.B. The retry count parameter is named '$retryCount'; you should avoid using this variable name if you want to use this annotation.

By default will catch any *Exception* and use retry logic once, but has parameters to control it:

* maxRetries
  * The maximum amount of times a method can be retried. This is *retried* not *attempted* so the first pass is not counted.
* includes
  * List of *Throwables* that will be caught and retried. Other exceptions will still throw.
* excludes
  * List of *Throwables* that will **not** be caught and retried. Other exceptions will not throw.
  
...See [Test](https://github.com/aataylor95/retry-ast/blob/master/src/test/groovy/com/github/aataylor95/ast/RetryASTTransformationSpec.groovy) for more documentation.



