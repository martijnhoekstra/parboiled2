/*
 * Copyright (C) 2009-2013 Mathias Doenitz, Alexander Myltsev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.parboiled2

class SimpleCalculatorSpec extends TestParserSpec {
  // SimpleCalculator from https://github.com/sirthias/parboiled/blob/master/examples-scala/src/main/scala/org/parboiled/examples/calculators/SimpleCalculator0.scala
  abstract class SimpleCalculator extends TestParser {
    def InputLine = rule { Expression ~ EOI }

    def Expression: Rule = rule { Term ~ zeroOrMore((ch('+') | '-') ~ Term) }

    def Term = rule { Factor ~ zeroOrMore((ch('*') | '/') ~ Factor) }

    def Factor = rule { Digits | Parens }

    def Parens = rule { "(" ~ Expression ~ ")" }

    def Digits = rule { oneOrMore(Digit) }

    def Digit = rule { ch('0') | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' }
  }

  "A SimpleCalculator" should {
    "successfully recognize expression" in new SimpleCalculator {
      def targetRule = InputLine

      "1" must Match
      "1+2" must Match
      "1+2*3" must Match
      "1*2+3" must Match
      "1*(2+3)" must Match
      "1*((2+3))" must Match

      "*1" must Mismatch
      "+1" must Mismatch
      "()" must Mismatch
      "(()" must Mismatch
      "())" must Mismatch
      "(1+)2" must Mismatch
    }
  }
}