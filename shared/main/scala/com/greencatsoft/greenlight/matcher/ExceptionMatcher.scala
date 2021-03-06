package com.greencatsoft.greenlight.matcher

import scala.reflect.ClassTag

import com.greencatsoft.greenlight.TestFailureException
import com.greencatsoft.greenlight.grammar.CodeBlock
import com.greencatsoft.greenlight.grammar.Object.Expectation
import com.greencatsoft.greenlight.grammar.PassiveVerb
import com.greencatsoft.greenlight.grammar.Specification.WhatIsExpected
import com.greencatsoft.greenlight.grammar.Statement.Assertation
import com.greencatsoft.greenlight.grammar.Verb.FollowedByNegation

import ExceptionMatcher.BeThrownIn

trait ExceptionMatcher extends Matcher[ClassTag[_], BeThrownIn, CodeBlock[_]] {

  override def matches(actual: ClassTag[_], expected: CodeBlock[_]) {
    val expectedType = actual.runtimeClass

    try {
      expected()

      throw TestFailureException(s"Expected '$expectedType' to be thrown but it wasn't.")
    } catch {
      case t: TestFailureException => throw t
      case t: Throwable if expectedType.isAssignableFrom(t.getClass) =>
      case t: Throwable => rethrow(t)
    }
  }

  override def notMatches(actual: ClassTag[_], expected: CodeBlock[_]) {
    val expectedType = actual.runtimeClass

    try {
      expected()
    } catch {
      case t: Throwable if expectedType.isAssignableFrom(t.getClass) =>
        throw TestFailureException(s"Expected '$expectedType' not to be thrown but it was.")
      case e: Throwable => rethrow(e)
    }
  }

  private def rethrow(t: Throwable): Unit = t match {
    case e: RuntimeException => throw e
    case e: Error => throw e
    case e => throw new RuntimeException(e)
  }
}

object ExceptionMatcher {

  class BeThrownIn extends PassiveVerb {

    override def description: String = "be thrown in"

    def apply[A](block: => Any)(implicit matcher: Matcher[A, BeThrownIn, CodeBlock[_]]) =
      WhatIsExpected(this, Expectation(new CodeBlock(() => block)))
  }

  trait Words {

    object be_thrown_in extends BeThrownIn
  }

  object Words extends Words

  trait Conversions {

    implicit class NotThrownIn[A](fbn: FollowedByNegation[A]) {

      def be_thrown_in(block: => Any)(
        implicit matcher: Matcher[A, BeThrownIn, CodeBlock[_]]): Assertation[A, BeThrownIn, CodeBlock[_]] =
        fbn.builder.assert(WhatIsExpected(Words.be_thrown_in, !Expectation(new CodeBlock(() => block))))
    }
  }
}