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

import scala.reflect.internal.annotations.compileTimeOnly
import org.parboiled2.support._
import shapeless.HList

trait RuleDSLBasics {

  /**
   * Matches the given single character.
   */
  @compileTimeOnly("Calls to `ch` must be inside `rule` macro")
  implicit def ch(c: Char): Rule0 = `n/a`

  /**
   * Matches the given string of characters.
   */
  @compileTimeOnly("Calls to `str` must be inside `rule` macro")
  implicit def str(s: String): Rule0 = `n/a`

  /**
   * Matches any (single) character matched by the given `CharPredicate`.
   */
  @compileTimeOnly("Calls to `predicate` must be inside `rule` macro")
  implicit def predicate(p: CharPredicate): Rule0 = `n/a`

  /**
   * Matches any of the given maps keys and pushes the respective value upon
   * a successful match.
   */
  @compileTimeOnly("Calls to `valueMap` must be inside `rule` macro")
  implicit def valueMap[T](m: Map[String, T])(implicit h: HListable[T]): RuleN[h.Out] = `n/a`

  /**
   * Matches a character and pushes the associated value.
   */
  @compileTimeOnly("Calls to `charAndValue` must be inside `rule` macro")
  implicit def charAndValue[T](t: (Char, T))(implicit h: HListable[T]): RuleN[h.Out] = `n/a`

  /**
   * Matches a string and pushes the associated value.
   */
  @compileTimeOnly("Calls to `stringAndValue` must be inside `rule` macro")
  implicit def stringAndValue[T](t: (String, T))(implicit h: HListable[T]): RuleN[h.Out] = `n/a`

  /**
   * Matches any single one of the given characters.
   */
  @compileTimeOnly("Calls to `anyOf` must be inside `rule` macro")
  def anyOf(chars: String): Rule0 = `n/a`

  /**
   * Matches the given single character case insensitively.
   * Note: the given character must be specified in lower-case!
   * This requirement is currently NOT enforced!
   */
  @compileTimeOnly("Calls to `ignoreCase` must be inside `rule` macro")
  def ignoreCase(c: Char): Rule0 = `n/a`

  /**
   * Matches the given string of characters case insensitively.
   * Note: the given string must be specified in all lower-case!
   * This requirement is currently NOT enforced!
   */
  @compileTimeOnly("Calls to `ignoreCase` must be inside `rule` macro")
  def ignoreCase(s: String): Rule0 = `n/a`

  /**
   * Matches any character except EOI.
   */
  @compileTimeOnly("Calls to `ANY` must be inside `rule` macro")
  def ANY: Rule0 = `n/a`

  /**
   * Matches the EOI (end-of-input) character.
   */
  def EOI: Char = org.parboiled2.EOI

  /**
   * Matches no character (i.e. doesn't cause the parser to make any progress) but succeeds always (as a rule).
   */
  def MATCH: Rule0 = Rule

  /**
   * A rule that always fails.
   */
  def MISMATCH[I <: HList, O <: HList]: Rule[I, O] = null

  @compileTimeOnly("Calls to `str2CharRangeSupport` must be inside `rule` macro")
  implicit def str2CharRangeSupport(s: String): CharRangeSupport = `n/a`
  sealed trait CharRangeSupport {
    def -(other: String): Rule0
  }
}
