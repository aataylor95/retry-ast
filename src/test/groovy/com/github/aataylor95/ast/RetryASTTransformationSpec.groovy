package com.github.aataylor95.ast

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import spock.lang.Specification

class RetryASTTransformationSpec extends Specification {
  final GroovyShell shell = new GroovyShell(this.class.classLoader)

  void "retry method once for any exception by default"() {
    given:
      String text = '''
       import com.github.aataylor95.ast.Retry
      
        class TemperamentalService {
         Integer counter = 0
        
         @Retry
         Integer doIt() {
           //println "Attempt ${counter + 1}"
           
           if (counter == 0) {
            counter ++
            throw new Exception()
           }
           
           return counter
         }
        }
        
        return new TemperamentalService().doIt()
      '''
    when:
      Integer retries = shell.evaluate(text)
    then:
      retries == 1
  }

  void "retry method up too the amount of times specified by maxRetries"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(maxRetries = 10)
          Integer doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter < 5) {
              counter++
              throw new Exception()
            }
            
            return counter
          }            
        }
  
        return new TemperamentalService().doIt()
      '''
    when:
      Integer retries = shell.evaluate(text)
    then:
      retries == 5
  }

  void "don't retry past the number of maxRetries"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(maxRetries = 10)
          Integer doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter < 50) {
              counter++
              throw new Exception()
            }
            
            return counter
          }            
        }
        
        def service = new TemperamentalService()

        try {
          service.doIt()
        } catch(Exception e) {
          return service.counter
        }
      '''
    when:
      Integer attempts = shell.evaluate(text)
    then:
      attempts == 11 // Including first try
  }

  void "catch specified exceptions in includes"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(includes = [FileNotFoundException])
          boolean doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter == 0) {
              counter++
              throw new FileNotFoundException()
            }
            
            return true
          }            
        }

        return new TemperamentalService().doIt()
      '''
    expect:
      shell.evaluate(text)
  }

  void "don't catch non-specified exceptions if includes is defined"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(includes = [FileNotFoundException])
          boolean doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter == 0) {
              counter++
              throw new NullPointerException()
            }
            
            return true
          }            
        }

        return new TemperamentalService().doIt()
      '''
    when:
      shell.evaluate(text)
    then:
      thrown NullPointerException
  }

  void "don't catch specified exceptions in excludes"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(excludes = [FileNotFoundException])
          boolean doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter == 0) {
              counter++
              throw new FileNotFoundException()
            }
            
            return true
          }            
        }

        return new TemperamentalService().doIt()
      '''
    when:
      shell.evaluate(text)
    then:
      thrown FileNotFoundException
  }

  void "catch exception not specified in excludes"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(excludes = [FileNotFoundException])
          boolean doIt() {
            //println "Attempt ${counter + 1}"
            
            if (counter == 0) {
              counter++
              throw new NullPointerException()
            }
            
            return true
          }            
        }

        return new TemperamentalService().doIt()
      '''
    expect:
      shell.evaluate(text)
  }

  void "can't have non-exceptions in the includes/excludes list"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
        
        class TemperamentalService {
          Integer counter = 0
          
          @Retry(includes = [FileNotFoundException, Integer])
          boolean doIt() {
            println "Shouldn't Get Here"
          }            
        }

        return new TemperamentalService().doIt()
       '''
    when:
      shell.evaluate(text)
    then:
      MultipleCompilationErrorsException e = thrown()
      e.message.contains("'includes' contains Classes that aren't Throwables: [Integer]")
  }

  void "max retries can't be less than 1"() {
    given:
      String text = '''
       import com.github.aataylor95.ast.Retry
      
        class TemperamentalService {
         Integer counter = 0
        
         @Retry(maxRetries = -1)
         Integer doIt() {
           println "Shouldn't get here"
         }
        }
        
        return new TemperamentalService().doIt()
      '''
    when:
      shell.evaluate(text)
    then:
      MultipleCompilationErrorsException e = thrown()
      e.message.contains("'maxRetries' must be a positive integer")
  }

  void "report multiple errors"() {
    given:
      String text = '''
       import com.github.aataylor95.ast.Retry
      
        class TemperamentalService {
         Integer counter = 0
        
         @Retry(maxRetries = -1, includes = [String])
         Integer doIt() {
           println "Shouldn't get here"
         }
        }
        
        return new TemperamentalService().doIt()
      '''
    when:
      shell.evaluate(text)
    then:
      MultipleCompilationErrorsException e = thrown()
      e.errorCollector.errorCount == 2
      e.message.contains("'includes' contains Classes that aren't Throwables: [String]")
      e.message.contains("'maxRetries' must be a positive integer")
  }

  void "wait an arbitrary time period before retrying"() {
    given:
      String text = '''
        import com.github.aataylor95.ast.Retry
      
        class TemperamentalService {
         long beforeDelay = 0
        
         @Retry(delayInMillis = 3000)
         Integer doIt() {
           //println "Attempt ${counter + 1}"
           
           if (beforeDelay == 0) {
            beforeDelay = System.currentTimeMillis()
            throw new Exception()
           }
           
           return System.currentTimeMillis() - beforeDelay
         }
        }
        
        return new TemperamentalService().doIt()
      '''
    when:
      Integer delay = shell.evaluate(text)
    then:
      delay > 3000
      delay < 4000
  }
}
