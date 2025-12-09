package zest.core.parser

import zest.core.ast.*
import zest.core.errors.TemplateError

/** Parser for Zest template syntax using simple recursive descent */
object TemplateParser {

  case class ParseState(input: String, pos: Int = 0) {
    def current: Option[Char] = if (pos < input.length) Some(input(pos)) else None
    def next: ParseState = copy(pos = pos + 1)
    def take(n: Int): ParseState = copy(pos = pos + n)
    def remaining: String = input.drop(pos)
    def substring(end: Int): String = input.substring(pos, end)
  }

  private val templateName = "template"

  private def location(state: ParseState): SourceLocation =
    SourceLocation(templateName, 1, state.pos)

  // Basic parsing functions
  private def parseChar(expected: Char)(state: ParseState): Either[TemplateError, ParseState] = {
    state.current match {
      case Some(c) if c == expected => Right(state.next)
      case Some(c) => Left(TemplateError.ParseError(
        s"Expected '$expected' but found '$c'",
        templateName,
        Some((1, state.pos))
      ))
      case None => Left(TemplateError.ParseError(
        s"Unexpected end of input, expected '$expected'",
        templateName,
        Some((1, state.pos))
      ))
    }
  }

  private def parseString(expected: String)(state: ParseState): Either[TemplateError, ParseState] = {
    if (state.remaining.startsWith(expected)) {
      Right(state.take(expected.length))
    } else {
      Left(TemplateError.ParseError(
        s"Expected '$expected'",
        templateName,
        Some((1, state.pos))
      ))
    }
  }

  private def parseWhile(predicate: Char => Boolean)(state: ParseState): (String, ParseState) = {
    val builder = StringBuilder()
    var current = state
    while (current.current.exists(predicate)) {
      builder += current.current.get
      current = current.next
    }
    (builder.toString(), current)
  }

  private def parseIdentifier(state: ParseState): Either[TemplateError, (String, ParseState)] = {
    val (name, newState) = parseWhile(c => c.isLetterOrDigit || c == '_')(state)
    if (name.nonEmpty) Right((name, newState)) else Left(TemplateError.ParseError(
      "Expected identifier",
      templateName,
      Some((1, state.pos))
    ))
  }

  private def parseWhitespace(state: ParseState): ParseState = {
    parseWhile(_.isWhitespace)(state)._2
  }

  // Template delimiters
  private val DoubleLeft = "{{"
  private val DoubleRight = "}}"
  private val BlockLeft = "{%"
  private val BlockRight = "%}"
  private val CommentLeft = "{#"
  private val CommentRight = "#}"

  // Keywords
  private def isKeyword(word: String): Boolean = word match {
    case "if" | "else" | "elif" | "endif" | "for" | "endfor" | "true" | "false" | "null" => true
    case _ => false
  }

  // Expression parsing
  private def parseExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseOrExpression(state)
  }

  private def parseOrExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseAndExpression(state).flatMap {
      case (left, state1) =>
        state1.current match {
          case Some('&') if state1.remaining.startsWith("||") =>
            parseAndExpression(state1.take(2)).flatMap {
              case (right, state2) =>
                parseOrExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "||", right, location(state)), finalState)
                }
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseAndExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseEqualityExpression(state).flatMap {
      case (left, state1) =>
        state1.current match {
          case Some('&') if state1.remaining.startsWith("&&") =>
            parseEqualityExpression(state1.take(2)).flatMap {
              case (right, state2) =>
                parseAndExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "&&", right, location(state)), finalState)
                }
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseEqualityExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseComparisonExpression(state).flatMap {
      case (left, state1) =>
        state1.remaining match {
          case s if s.startsWith("==") =>
            parseComparisonExpression(state1.take(2)).map {
              case (right, state2) => (BinaryOperation(left, "==", right, location(state)), state2)
            }
          case s if s.startsWith("!=") =>
            parseComparisonExpression(state1.take(2)).map {
              case (right, state2) => (BinaryOperation(left, "!=", right, location(state)), state2)
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseComparisonExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseAdditiveExpression(state).flatMap {
      case (left, state1) =>
        state1.remaining match {
          case s if s.startsWith("<=") =>
            parseAdditiveExpression(state1.take(2)).map {
              case (right, state2) => (BinaryOperation(left, "<=", right, location(state)), state2)
            }
          case s if s.startsWith(">=") =>
            parseAdditiveExpression(state1.take(2)).map {
              case (right, state2) => (BinaryOperation(left, ">=", right, location(state)), state2)
            }
          case s if s.startsWith("<") =>
            parseAdditiveExpression(state1.take(1)).map {
              case (right, state2) => (BinaryOperation(left, "<", right, location(state)), state2)
            }
          case s if s.startsWith(">") =>
            parseAdditiveExpression(state1.take(1)).map {
              case (right, state2) => (BinaryOperation(left, ">", right, location(state)), state2)
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseAdditiveExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseMultiplicativeExpression(state).flatMap {
      case (left, state1) =>
        state1.current match {
          case Some('+') =>
            parseMultiplicativeExpression(state1.next).flatMap {
              case (right, state2) =>
                parseAdditiveExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "+", right, location(state)), finalState)
                }
            }
          case Some('-') =>
            parseMultiplicativeExpression(state1.next).flatMap {
              case (right, state2) =>
                parseAdditiveExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "-", right, location(state)), finalState)
                }
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseMultiplicativeExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseUnaryExpression(state).flatMap {
      case (left, state1) =>
        state1.current match {
          case Some('*') =>
            parseUnaryExpression(state1.next).flatMap {
              case (right, state2) =>
                parseMultiplicativeExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "*", right, location(state)), finalState)
                }
            }
          case Some('/') =>
            parseUnaryExpression(state1.next).flatMap {
              case (right, state2) =>
                parseMultiplicativeExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "/", right, location(state)), finalState)
                }
            }
          case Some('%') =>
            parseUnaryExpression(state1.next).flatMap {
              case (right, state2) =>
                parseMultiplicativeExpression(state2).map {
                  case (expr, finalState) =>
                    (BinaryOperation(left, "%", right, location(state)), finalState)
                }
            }
          case _ => Right((left, state1))
        }
    }
  }

  private def parseUnaryExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    state.current match {
      case Some('!') =>
        parseUnaryExpression(state.next).map {
          case (operand, newState) => (UnaryOperation("!", operand, location(state)), newState)
        }
      case Some('-') =>
        parseUnaryExpression(state.next).map {
          case (operand, newState) => (UnaryOperation("-", operand, location(state)), newState)
        }
      case _ => parsePrimaryExpression(state)
    }
  }

  private def parsePrimaryExpression(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    state.current match {
      case Some('"') =>
        parseStringLiteral(state.next)
      case Some(c) if c.isDigit || c == '.' =>
        parseNumberLiteral(state)
      case Some(c) if c.isLetter || c == '_' =>
        parseVariableOrKeyword(state)
      case Some('(') =>
        parseExpression(state.next).flatMap {
          case (expr, state1) =>
            parseChar(')')(state1).map {
              case newState => (expr, newState)
            }
        }
      case _ =>
        Left(TemplateError.ParseError(
          "Expected expression",
          templateName,
          Some((1, state.pos))
        ))
    }
  }

  private def parseStringLiteral(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    val (content, newState) = parseWhile(_ != '"')(state)
    newState.current match {
      case Some('"') =>
        Right((StringLiteral(content, location(state)), newState.next))
      case _ =>
        Left(TemplateError.ParseError(
          "Unterminated string literal",
          templateName,
          Some((1, state.pos))
        ))
    }
  }

  private def parseNumberLiteral(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    val (numberStr, newState) = parseWhile(c => c.isDigit || c == '.')(state)
    try {
      val number = numberStr.toDouble
      Right((NumberLiteral(number, location(state)), newState))
    } catch {
      case _: NumberFormatException =>
        Left(TemplateError.ParseError(
          s"Invalid number: $numberStr",
          templateName,
          Some((1, state.pos))
        ))
    }
  }

  private def parseVariableOrKeyword(state: ParseState): Either[TemplateError, (Expression, ParseState)] = {
    parseIdentifier(state).flatMap {
      case (name, newState) =>
        if (isKeyword(name)) {
          name match {
            case "true" => Right((BooleanLiteral(true, location(state)), newState))
            case "false" => Right((BooleanLiteral(false, location(state)), newState))
            case "null" => Right((StringLiteral("null", location(state)), newState))
            case _ => Left(TemplateError.ParseError(
              s"Unexpected keyword: $name",
              templateName,
              Some((1, state.pos))
            ))
          }
        } else {
          Right((VariableReference(name, location(state)), newState))
        }
    }
  }

  // Statement parsing
  private def parseStatement(state: ParseState): Either[TemplateError, (TemplateNode, ParseState)] = {
    state.remaining match {
      case s if s.startsWith(CommentLeft) =>
        parseComment(state)
      case s if s.startsWith(BlockLeft) =>
        parseBlockStatement(state)
      case s if s.startsWith(DoubleLeft) =>
        parseInterpolation(state)
      case _ =>
        parseText(state)
    }
  }

  private def parseComment(state: ParseState): Either[TemplateError, (TemplateNode, ParseState)] = {
    parseString(CommentLeft)(state).flatMap { state1 =>
      val (content, state2) = parseWhile(_ != '}')(state1)
      parseString(CommentRight)(state2).map {
        case newState => (Comment(content, location(state)), newState)
      }
    }
  }

  private def parseInterpolation(state: ParseState): Either[TemplateError, (TemplateNode, ParseState)] = {
    parseString(DoubleLeft)(state).flatMap { state1 =>
      val state2 = parseWhitespace(state1)
      parseExpression(state2).flatMap {
        case (expr, state3) =>
          val state4 = parseWhitespace(state3)
          parseString(DoubleRight)(state4).map {
            case newState => (Interpolation(expr, location(state)), newState)
          }
      }
    }
  }

  private def parseBlockStatement(state: ParseState): Either[TemplateError, (TemplateNode, ParseState)] = {
    parseString(BlockLeft)(state).flatMap { state1 =>
      val state2 = parseWhitespace(state1)
      parseIdentifier(state2).flatMap {
        case (keyword, state3) =>
          keyword match {
            case "if" =>
              val state4 = parseWhitespace(state3)
              parseExpression(state4).flatMap {
                case (condition, state5) =>
                  val state6 = parseWhitespace(state5)
                  parseString(BlockRight)(state6).flatMap { state7 =>
                    parseBody(state7).flatMap {
                      case (body, state8) =>
                        parseElseOrEndIf(state8).map {
                          case (elseBody, finalState) =>
                            (IfStatement(condition, body, elseBody, location(state)), finalState)
                        }
                    }
                  }
              }
            case _ =>
              Left(TemplateError.ParseError(
                s"Unknown block statement: $keyword",
                templateName,
                Some((1, state.pos))
              ))
          }
      }
    }
  }

  private def parseElseOrEndIf(state: ParseState): Either[TemplateError, (Option[List[TemplateNode]], ParseState)] = {
    val state1 = parseWhitespace(state)
    state1.remaining match {
      case s if s.startsWith(BlockLeft + " else") =>
        parseString(BlockLeft)(state1).flatMap { state2 =>
          parseString("else")(state2).flatMap { state3 =>
            val state4 = parseWhitespace(state3)
            parseString(BlockRight)(state4).flatMap { state5 =>
              parseBody(state5).flatMap {
                case (elseBody, state6) =>
                  parseString(BlockLeft)(state6).flatMap { state7 =>
                    parseString("endif")(state7).flatMap { state8 =>
                      val state9 = parseWhitespace(state8)
                      parseString(BlockRight)(state9).map {
                        case finalState => (Some(elseBody), finalState)
                      }
                    }
                  }
              }
            }
          }
        }
      case s if s.startsWith(BlockLeft + " endif") =>
        parseString(BlockLeft)(state1).flatMap { state2 =>
          parseString("endif")(state2).flatMap { state3 =>
            val state4 = parseWhitespace(state3)
            parseString(BlockRight)(state4).map {
              case finalState => (None, finalState)
            }
          }
        }
      case _ =>
        Left(TemplateError.ParseError(
          "Expected {% else %} or {% endif %}",
          templateName,
          Some((1, state.pos))
        ))
    }
  }

  private def parseBody(state: ParseState): Either[TemplateError, (List[TemplateNode], ParseState)] = {
    def parseBodyRec(state: ParseState, acc: List[TemplateNode]): Either[TemplateError, (List[TemplateNode], ParseState)] = {
      state.remaining match {
        case s if s.isEmpty || s.startsWith(BlockLeft) =>
          Right((acc.reverse, state))
        case _ =>
          parseStatement(state).flatMap {
            case (node, newState) => parseBodyRec(newState, node :: acc)
          }
      }
    }
    parseBodyRec(state, Nil)
  }

  private def parseText(state: ParseState): Either[TemplateError, (TemplateNode, ParseState)] = {
    val (content, newState) = parseWhile(c => !c.isControl && !state.remaining.startsWith("{{") && !state.remaining.startsWith("{%") && !state.remaining.startsWith("{#"))(state)
    if (content.nonEmpty) {
      Right((TextNode(content, location(state)), newState))
    } else {
      Left(TemplateError.ParseError(
        "Unexpected character",
        templateName,
        Some((1, state.pos))
      ))
    }
  }

  // Main template parsing
  private def parseTemplateBody(state: ParseState): Either[TemplateError, (List[TemplateNode], ParseState)] = {
    def parseTemplateRec(state: ParseState, acc: List[TemplateNode]): Either[TemplateError, (List[TemplateNode], ParseState)] = {
      if (state.remaining.isEmpty) {
        Right((acc.reverse, state))
      } else {
        parseStatement(state).flatMap {
          case (node, newState) => parseTemplateRec(newState, node :: acc)
        }
      }
    }
    parseTemplateRec(state, Nil)
  }

  // Public API
  def parse(template: String): Either[TemplateError, Template] = {
    val state = ParseState(template)
    parseTemplateBody(state).map {
      case (nodes, _) => Template(templateName, nodes)
    }
  }
}
