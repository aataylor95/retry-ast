package com.github.aataylor95.ast

import org.apache.groovy.ast.tools.ClassNodeUtils
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.TryCatchStatement

import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.*

class RetryASTSidecar {
  static final String RETRIES = '$retryCount'
  static final Closure<Boolean> nonException = { ClassNode c -> !(Class.forName(c.name) in Throwable) }

  //Add a retry method to the method owner with an extra parameter for $retryCount (overloading the original method)
  static MethodNode createRetryMethod(MethodNode method) {
    Parameter[] newParams = params(*method.parameters, param(make(Integer), RETRIES))

    MethodNode retryMethod = method.with { new MethodNode(name, modifiers, returnType, newParams, exceptions, code) }

    ClassNodeUtils.addGeneratedMethod(method.declaringClass, retryMethod)

    return retryMethod
  }

  //Will call retry method with $retryCount + 1
  static BlockStatement createRetryCall(MethodNode method, Integer delayInMillis, boolean initialMethod = false) {
    List<Expression> parameters = method.parameters.collect { varX(it.name) }
    parameters << (initialMethod ? constX(0) : plusX(varX(RETRIES), constX(1)))

    Statement methodCall = stmt(callThisX(method.name, args(parameters)))

    if (delayInMillis) {
      Statement threadSleep = stmt(callX(make(Thread), 'sleep', args(constX(delayInMillis))))

      return block(threadSleep, methodCall)
    }

    return block(methodCall)
  }

  //Wrap existing method statements in a try/catch
  static TryCatchStatement createTryCatch(MethodNode method) {
    return new TryCatchStatement(block(method.variableScope, method.code), new BlockStatement())
  }

  //Create a catch for exception that retries
  static CatchStatement createCatchRetry(ClassNode exception, BlockStatement retryCall, Integer maxRetries = null) {
    String exceptionName = name(exception)
    Parameter exceptionParam = param(exception, exceptionName)

    if (maxRetries) {
      BinaryExpression retryCountLessThanMax = ltX(varX(RETRIES), constX(maxRetries - 1))

      return catchS(exceptionParam, ifElseS(retryCountLessThanMax, retryCall, throwS(varX(exceptionName))))
    }

    return catchS(exceptionParam, retryCall)
  }

  //Create a catch for exception that re-throws exception; for retrying all exceptions except excludes
  static CatchStatement createCatchThrow(ClassNode exception) {
    String exceptionName = name(exception)

    return catchS(param(exception, exceptionName), throwS(varX(exceptionName)))
  }

  static CatchStatement createCatchAll(BlockStatement retryCall, Integer maxRetries = null) {
    return createCatchRetry(make(Exception), retryCall, maxRetries)
  }

  static void reportClassError(List<ClassNode> classes, String name, Closure addError) {
    if (classes && classes.any(nonException)) {
      List<String> nonExceptions = classes.findAll(nonException)*.nameWithoutPackage

      addError("'$name' contains Classes that aren't Throwables: $nonExceptions")
    }
  }

  private static String name(ClassNode clazz) {
    return clazz.nameWithoutPackage.uncapitalize()
  }
}
