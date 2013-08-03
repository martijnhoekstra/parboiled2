package org.parboiled2

trait OpTreeContext[OpTreeCtx <: Parser.ParserContext] {
  val c: OpTreeCtx
  import c.universe._

  abstract class OpTree {
    def render(ruleName: String = ""): Expr[Rule]
  }

  object OpTree {
    def apply(tree: Tree): OpTree = tree match {
      case Combinator(x @ (Sequence() | FirstOf())) ⇒ x.opTree.get
      case Modifier(x @ (LiteralString() | LiteralChar() | Optional() | ZeroOrMore() | OneOrMore() | AndPredicate())) ⇒ x.opTree.get
      case RuleCall(x) ⇒ x
      case NotPredicate(x) ⇒ x
      case CharacterClass(x) ⇒ x
      case _ ⇒ c.abort(tree.pos, s"Invalid rule definition: $tree\n${showRaw(tree)}")
    }
  }

  object Combinator {
    case class TreeMatch(lhs: Tree, methodName: String, rhs: Tree) { var opTree: Option[OpTree] = None }
    def unapply(tree: Tree): Option[TreeMatch] = tree match {
      case Apply(Select(lhs, Decoded(methodName)), List(rhs)) ⇒ Some(TreeMatch(lhs, methodName, rhs))
      case _ ⇒ None
    }
    abstract class Companion(methodName: String) {
      def apply(lhs: OpTree, rhs: OpTree): OpTree
      def unapply(tm: TreeMatch): Boolean =
        if (tm.methodName == methodName) {
          val lhs = OpTree(tm.lhs)
          val rhs = OpTree(tm.rhs)
          tm.opTree = Some(apply(lhs, rhs))
          true
        } else false
    }
  }

  object Modifier {
    case class TreeMatch(methodName: String, arg: Tree) { var opTree: Option[OpTree] = None }
    def unapply(tree: Tree): Option[TreeMatch] = tree match {
      case Apply(Select(This(_), Decoded(methodName)), List(arg)) ⇒ Some(TreeMatch(methodName, arg))
      case _ ⇒ None
    }
    abstract class Companion {
      def fromTreeMatch: PartialFunction[TreeMatch, OpTree]
      def unapply(tm: TreeMatch): Boolean =
        // applyOrElse is faster then `isDefined` + `apply`
        fromTreeMatch.applyOrElse(tm, (_: AnyRef) ⇒ null) match {
          case null ⇒ false
          case opTree ⇒
            tm.opTree = Some(opTree)
            true
        }
    }
  }

  case class CharacterClass(lowerStr: LiteralString, upperStr: LiteralString) extends OpTree {
    require(lowerStr.s.length == 1, "lower bound must be a single char string")
    require(upperStr.s.length == 1, "upper bound must be a single char string")
    val lower = lowerStr.s.charAt(0)
    val upper = upperStr.s.charAt(0)
    require(lower <= upper, "lower bound must not be > upper bound")

    def render(ruleName: String): Expr[Rule] = reify {
      try {
        val p = c.prefix.splice
        val char = p.nextChar()
        if (c.literal(lower).splice <= char && char <= c.literal(upper).splice) Rule.success
        else {
          p.onCharMismatch()
          Rule.failure
        }
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.CharacterClass(lower, upper, c.literal(ruleName).splice))
      }
    }
  }
  object CharacterClass {
    def unapply(tree: Tree): Option[OpTree] =
      tree match {
        case Apply(Select(lhs: LiteralString, Decoded("-")), List(rhs: LiteralString)) ⇒ Some(apply(lhs, rhs))
        case _ ⇒ None
      }
  }

  // TODO: Having sequence be a simple (lhs, rhs) model causes us to allocate a mark on the stack
  // for every sequence concatenation. If we modeled sequences as a Seq[OpTree] we would be able to
  // reuse a single mutable mark for all intermediate markings in between elements. This will reduce
  // the stack size for all rules with sequences that are more than two elements long.
  case class Sequence(lhs: OpTree, rhs: OpTree) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      try Rule(lhs.render().splice.matched && rhs.render().splice.matched)
      catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.Sequence(c.literal(ruleName).splice))
      }
    }
  }
  object Sequence extends Combinator.Companion("~")

  case class FirstOf(lhs: OpTree, rhs: OpTree) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      try {
        val p = c.prefix.splice
        val mark = p.mark
        if (lhs.render().splice.matched) Rule.success
        else {
          p.reset(mark)
          Rule(rhs.render().splice.matched)
        }
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.FirstOf(c.literal(ruleName).splice))
      }
    }
  }
  object FirstOf extends Combinator.Companion("|")

  case class LiteralString(s: String) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      val string = c.literal(s).splice
      try {
        val p = c.prefix.splice
        var ix = 0
        while (ix < string.length && p.nextChar() == string.charAt(ix)) ix += 1
        if (ix == string.length) Rule.success
        else {
          p.onCharMismatch()
          Rule.failure
        }
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.LiteralString(string, c.literal(ruleName).splice))
      }
    }
  }
  object LiteralString extends Modifier.Companion {
    // TODO: expand string literal into sequence of LiteralChars for all strings below a certain threshold
    // number of characters (i.e. we "unroll" short strings with, say, less than 16 chars)
    def fromTreeMatch = {
      case Modifier.TreeMatch("str", Literal(Constant(s: String))) ⇒ LiteralString(s)
    }
  }

  case class LiteralChar(ch: Char) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      val char = c.literal(ch).splice
      try {
        val p = c.prefix.splice
        if (p.nextChar() == char) Rule.success
        else {
          p.onCharMismatch()
          Rule.failure
        }
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.LiteralChar(char, c.literal(ruleName).splice))
      }
    }
  }
  object LiteralChar extends Modifier.Companion {
    def fromTreeMatch = {
      case Modifier.TreeMatch("ch", Select(This(_), Decoded("EOI"))) ⇒ LiteralChar(Parser.EOI)
      case Modifier.TreeMatch("ch", Literal(Constant(c: Char)))      ⇒ LiteralChar(c)
    }
  }

  case class Optional(op: OpTree) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      try {
        val p = c.prefix.splice
        val mark = p.mark
        if (!op.render().splice.matched) p.reset(mark)
        Rule.success
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.Optional(c.literal(ruleName).splice))
      }
    }
  }
  object Optional extends Modifier.Companion {
    def fromTreeMatch = {
      case Modifier.TreeMatch("optional", arg) ⇒ Optional(OpTree(arg))
    }
  }

  case class ZeroOrMore(op: OpTree) extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      try {
        val p = c.prefix.splice
        var mark = p.mark
        while (op.render().splice.matched) { mark = p.mark }
        p.reset(mark)
        Rule.success
      } catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.ZeroOrMore(c.literal(ruleName).splice))
      }
    }
  }
  object ZeroOrMore extends Modifier.Companion {
    def fromTreeMatch = {
      case Modifier.TreeMatch("zeroOrMore", arg) ⇒ ZeroOrMore(OpTree(arg))
    }
  }

  object OneOrMore extends Modifier.Companion {
    def fromTreeMatch = {
      case Modifier.TreeMatch("oneOrMore", arg) ⇒
        val op = OpTree(arg)
        Sequence(op, ZeroOrMore(op))
    }
  }

  abstract class Predicate extends OpTree {
    def op: OpTree
    def renderMatch(): Expr[Boolean] = reify {
      val p = c.prefix.splice
      val mark = p.mark
      val matched = op.render().splice.matched
      p.reset(mark)
      matched
    }
  }

  case class AndPredicate(op: OpTree) extends Predicate {
    def render(ruleName: String): Expr[Rule] = reify {
      try Rule(renderMatch().splice)
      catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.AndPredicate(c.literal(ruleName).splice))
      }
    }
  }
  object AndPredicate extends Modifier.Companion {
    def fromTreeMatch = {
      case Modifier.TreeMatch("&", arg: Tree) ⇒ AndPredicate(OpTree(arg))
    }
  }

  case class NotPredicate(op: OpTree) extends Predicate {
    def render(ruleName: String): Expr[Rule] = reify {
      try Rule(!renderMatch().splice)
      catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.NotPredicate(c.literal(ruleName).splice))
      }
    }
  }
  object NotPredicate {
    def unapply(tree: Tree): Option[OpTree] = tree match {
      case Apply(Select(arg, Decoded("unary_!")), List()) ⇒ Some(NotPredicate(OpTree(arg)))
      case _ ⇒ None
    }
  }

  case class RuleCall(methodCall: Tree) extends OpTree {
    val calleeName = {
      val Select(This(tpName), termName) = methodCall
      s"$tpName.$termName"
    }
    def render(ruleName: String): Expr[Rule] = reify {
      try c.Expr[Rule](methodCall).splice
      catch {
        case e: Parser.CollectingRuleStackException ⇒
          e.save(RuleFrame.RuleCall(c.literal(ruleName).splice, c.literal(calleeName).splice))
      }
    }
  }
  object RuleCall {
    def unapply(tree: Tree): Option[OpTree] = tree match {
      case x @ Select(This(_), _)           ⇒ Some(RuleCall(x))
      case x @ Apply(Select(This(_), _), _) ⇒ Some(RuleCall(x))
      case _                                ⇒ None
    }
  }

  //  case class AnyCharacter() extends OpTree

  //  case class Grouping(n: OpTree) extends OpTree

  case object Empty extends OpTree {
    def render(ruleName: String): Expr[Rule] = reify {
      Rule.success
    }
  }

  ////////////////// helpers ///////////////////

  private object Decoded {
    def unapply(name: Name): Option[String] = Some(name.decoded)
  }
}
