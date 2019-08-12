# **\@Retry** - retry-ast

Catches exceptions and employs retry logic. Useful for things with an expected fault tolerance like S3 calls or other network traffic.

Works by injecting an overloaded method (wrapped in try/catch) with a retry count parameter and also wrapping exisiting method in a try/catch which can call the overloaded method

By default will catch any *Exception* and use retry logic once, has parameters to control it:

* maxRetries
  * The maximum amount of times a method can be retried. This is *retried* not *attempted* so the first pass is not counted.
* includes
  * List of *Throwables* that will be caught and retried. Other exceptions will still throw.
* excludes
  * List of *Throwables* that will **not** be caught and retried. Other exceptions will not throw.



