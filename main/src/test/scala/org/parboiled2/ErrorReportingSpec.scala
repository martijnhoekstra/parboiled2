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

class ErrorReportingSpec extends TestParserSpec {
  import RuleFrame._

  "Error reporting" should {
    "compose error messages for simple expression" in new TestParser {
      def targetRule = rule { oneOrMore("x") ~ ch('a') ~ 'b' ~ 'e' }

      parse("xxxace") ===
        Left(ParseError(Position(4, 1, 5), Seq(RuleStack(Seq(LiteralChar('b'), Sequence(), Sequence("targetRule"))))))
    }

    //    "compose error messages for simple calculator" in new SimpleCalculator {
    //      def targetRule = InputLine
    //
    //      "3+*5" must Mismatch
    //      run must beLeft
    //      run.left map { err ⇒
    //        err.position.line must_== 0
    //        err.position.column must_== 2
    //        //err.expectedRules must haveTheSameElementsAs(List("SimpleCalculator.Term")) // TODO: Fix veryfying of RulesStack
    //      }
    //    }
    //
    //    "track lines numbers" in {
    //      "zero line" in new TestParser {
    //        def targetRule: Rule = rule { str("a\n") ~ "b\n" ~ "c" }
    //
    //        "x\nb\nc" must Mismatch
    //        run must beLeft
    //        run.left map { err ⇒
    //          err.position.line must_== 0
    //          err.position.column must_== 0
    //        }
    //      }
    //
    //      "first line" in new TestParser {
    //        def targetRule: Rule = rule { str("a\n") ~ "b\n" ~ "c" }
    //
    //        "a\nx\nc" must Mismatch
    //        run must beLeft
    //        run.left map { err ⇒
    //          err.position.line must_== 1
    //          err.position.column must_== 1
    //        }
    //      }
    //
    //      "second line" in new TestParser {
    //        def targetRule: Rule = rule { str("a\n") ~ "b\n" ~ "c" }
    //
    //        "a\nb\nx" must Mismatch
    //        run must beLeft
    //        run.left map { err ⇒
    //          err.position.line must_== 2
    //          err.position.column must_== 1
    //        }
    //      }
    //    }
    //
    //    "correctly process FirstOf" in {
    //      "producing no errors for first alternative" in new TestParser {
    //        def targetRule: Rule = rule { ch('a') | 'b' }
    //
    //        "aaa" must Match
    //        run must beRight
    //      }
    //
    //      "producing no errors for second alternative" in new TestParser {
    //        def targetRule: Rule = rule { ch('a') | 'b' }
    //
    //        "b" must Match
    //        run must beRight
    //      }
    //    }
  }
}