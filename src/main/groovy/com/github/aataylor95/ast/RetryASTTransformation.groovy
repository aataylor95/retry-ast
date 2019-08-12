package com.github.aataylor95.ast

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static com.github.aataylor95.ast.RetryASTHelper.*

@GroovyASTTransformation(phase=CompilePhase.CANONICALIZATION)
class RetryASTTransformation extends AbstractASTTransformation {

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source)

    AnnotationNode anno = (AnnotationNode)nodes[0]
    MethodNode method = (MethodNode)nodes[1]

    //TODO: Why is it null when nothing is set, when default is 1?
    Integer maxRetries = getMemberIntValue(anno, MAX_RETRIES) ?: 1

    Closure error = this.&addError.rcurry(method)

    if (maxRetries < 0) {
      error("'maxRetries' must be a positive integer")
    }

    List<ClassNode> includes = getMemberClassList(anno, 'includes')
    List<ClassNode> excludes = getMemberClassList(anno, 'excludes')

    reportClassError(includes, 'includes', error)
    reportClassError(excludes, 'excludes', error)

    if (sourceUnit.errorCollector.errorCount) return

    MethodNode retryMethod = createRetryMethod(method)

    BlockStatement initialRetryCall = createRetryCall(method, true)
    BlockStatement retryCall = createRetryCall(method)

    TryCatchStatement initialTryCatch = createTryCatch(method)
    TryCatchStatement retryTryCatch = createTryCatch(retryMethod)

    CatchStatement initialCatchAll = createCatchAll(initialRetryCall)
    CatchStatement retryCatchAll = createCatchAll(retryCall, maxRetries)

    if (includes) {
      includes.each { ClassNode exception ->
        initialTryCatch.addCatch(createCatchRetry(exception, initialRetryCall))
        retryTryCatch.addCatch(createCatchRetry(exception, retryCall, maxRetries))
      }
    } else if (excludes) {
      excludes.each { ClassNode exception ->
        CatchStatement catchThrow  = createCatchThrow(exception)

        initialTryCatch.addCatch(catchThrow)
        retryTryCatch.addCatch(catchThrow)
      }

      initialTryCatch.addCatch(initialCatchAll)
      retryTryCatch.addCatch(retryCatchAll)
    } else {
      initialTryCatch.addCatch(initialCatchAll)
      retryTryCatch.addCatch(retryCatchAll)
    }

    method.code = block(method.variableScope, initialTryCatch)
    retryMethod.code = block(retryMethod.variableScope, retryTryCatch)
  }
}