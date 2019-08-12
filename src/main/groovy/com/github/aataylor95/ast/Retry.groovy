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

  Class[] excludes() default []

  Class[] includes() default []

  int maxRetries() default 1
}
