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

import shapeless.test.illTyped
import org.specs2.specification.Scope

class BasicSpec extends TestParserSpec {

  "The Parser should correctly recognize/reject input for" >> {

    "simple char literals" in new TestParser0 {
      def targetRule = rule { 'x' }
      "x" must beMatched
      "y" must beMismatched
      "" must beMismatched
    }

    "a simple char `val`" in new TestParser0 {
      val c = 'x'
      def targetRule = rule { c }
      "x" must beMatched
      "y" must beMismatched
      "" must beMismatched
    }

    "a simple char `def`" in new TestParser0 {
      def c = 'x'
      def targetRule = rule { c }
      "x" must beMatched
      "y" must beMismatched
      "" must beMismatched
    }

    "simple string literals" in new TestParser0 {
      def targetRule = rule { "ab" ~ EOI }
      "" must beMismatched
      "a" must beMismatched
      "ab" must beMatched
      "abc" must beMismatched
    }

    "a simple string `val`" in new TestParser0 {
      val s = "ab"
      def targetRule = rule { s ~ EOI }
      "" must beMismatched
      "a" must beMismatched
      "ab" must beMatched
      "abc" must beMismatched
    }

    "a simple string `def`" in new TestParser0 {
      def s = "ab"
      def targetRule = rule { s ~ EOI }
      "" must beMismatched
      "a" must beMismatched
      "ab" must beMatched
      "abc" must beMismatched
    }

    "a CharPredicate" in new TestParser0 {
      def targetRule = rule { CharPredicate.Digit }
      "0" must beMatched
      "8" must beMatched
      "x" must beMismatched
      "" must beMismatched
    }

    "anyOf" in new TestParser0 {
      def targetRule = rule { anyOf("abc") ~ EOI }
      "" must beMismatched
      "a" must beMatched
      "b" must beMatched
      "c" must beMatched
      "d" must beMismatched
      "ab" must beMismatched
    }

    "ignoreCase(char)" in new TestParser0 {
      def targetRule = rule { ignoreCase('x') ~ EOI }
      "" must beMismatched
      "x" must beMatched
      "X" must beMatched
      "y" must beMismatched
    }

    "ignoreCase(string)" in new TestParser0 {
      def targetRule = rule { ignoreCase("ab") ~ EOI }
      "" must beMismatched
      "a" must beMismatched
      "ab" must beMatched
      "Ab" must beMatched
      "aB" must beMatched
      "abc" must beMismatched
    }

    "ANY" in new TestParser0 {
      def targetRule = rule { ANY }
      "a" must beMatched
      "Ж" must beMatched
      "" must beMismatched
    }

    "EOI" in new TestParser0 {
      def targetRule = rule { EOI }
      "" must beMatched
      "x" must beMismatched
    }

    "character ranges" in new TestParser0 {
      def targetRule = rule { ("1" - "5") ~ EOI }
      "1" must beMatched
      "3" must beMatched
      "5" must beMatched
      "" must beMismatched
      "0" must beMismatched
      "a" must beMismatched
      "8" must beMismatched
    }

    "MATCH" in new TestParser0 {
      def targetRule = rule { MATCH ~ EOI }
      "" must beMatched
      "x" must beMismatched
    }

    "called rules" in new TestParser0 {
      def targetRule = {
        def free() = rule { "-free" }
        rule { foo ~ bar(42) ~ baz("", 1337) ~ free() ~ EOI }
      }
      def foo = rule { "foo" }
      def bar(i: Int) = rule { "-bar" ~ i.toString }
      def baz(s: String, i: Int) = rule { "-baz" ~ s ~ i.toString }
      "foo-bar42-baz1337-free" must beMatched
    }

    "Map[String, T]" in new TestParser1[Int] {
      val colors = Map("red" -> 1, "green" -> 2, "blue" -> 3)
      def targetRule = rule { colors ~ EOI }
      "red" must beMatchedWith(1)
      "green" must beMatchedWith(2)
      "blue" must beMatchedWith(3)
      "black" must beMismatched
    }

    "(Char, T)" in new TestParser1[Int] {
      def targetRule = rule { ('a' -> 42) ~ EOI }
      "a" must beMatchedWith(42)
      "x" must beMismatched
    }

    "(String, T)" in new TestParser1[Int] {
      def targetRule = rule { ("abc" -> 42) ~ EOI }
      "abc" must beMatchedWith(42)
      "x" must beMismatched
    }
  }

  "The Parser" should {
    "disallow compilation of an illegal character range" in new Parser with Scope {
      def input = ParserInput.Empty
      illTyped("""rule { "00" - "5" }""", "lower bound must be a single char string")
      illTyped("""rule { "0" - "55" }""", "upper bound must be a single char string")
      illTyped("""rule { "" - "5" }""", "lower bound must be a single char string")
      illTyped("""rule { "0" - "" }""", "upper bound must be a single char string")
      illTyped("""rule { "5" - "1" }""", "lower bound must not be > upper bound")
      success
    }
  }
}