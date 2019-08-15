package com.github.aataylor95.ast

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD])
@GroovyASTTransformationClass(classes = [RetryASTTransformation])
@interface Retry {

  //If specified, will only attempt to retry if an exception not in this list is thrown
  Class[] excludes() default []

  //If specified, will only attempt to retry if these exceptions are thrown
  Class[] includes() default []

  //Maximum number of times it will retry before giving up
  int maxRetries() default 1

  //Adds delay before attempting to retry
  int delayInMillis() default 0
}
