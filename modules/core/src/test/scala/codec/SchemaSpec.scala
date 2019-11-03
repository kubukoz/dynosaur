/*
 * Copyright 2019 OVO Energy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dynosaur
package codec

import cats.implicits._

import model.{AttributeName => Name, AttributeValue => Value}

// TODO change this to flatSpec
class SchemaSpec extends UnitSpec {
  /* simple case class */
  case class User(id: Int, name: String)
  /* nested case class */
  case class Role(capability: String, user: User)
  /* newtype */
  case class TraceToken(value: String)
  /* enum */
  sealed trait EventType
  case object Started extends EventType
  case object Completed extends EventType
  /* case class with enum */
  case class Event(state: EventType, value: String)
  /* ADT with case classes, objects and ambiguous cases */
  sealed trait Status
  case class Error(msg: String, cause: String) extends Status
  case class Warning(msg: String, cause: String) extends Status
  case object Unknown extends Status
  case class Successful(link: String, expires: Int) extends Status
  /* top level wrapper for ADT */
  case class Upload(id: String, status: Status)
  /* more than 22 fields, above tuple and function restriction */
  case class Big(
      one: String,
      two: String,
      three: String,
      four: String,
      five: String,
      six: String,
      seven: String,
      eight: String,
      nine: String,
      ten: String,
      eleven: String,
      twelve: String,
      thirteen: String,
      fourteen: String,
      fifteen: String,
      sixteen: String,
      seventeen: String,
      eighteen: String,
      nineteen: String,
      twenty: String,
      twentyOne: String,
      twentyTwo: String,
      twentyThree: String
  )

  def test[A](schema: Schema[A], data: A, expected: Value) = {
    def output = Encoder.fromSchema(schema).write(data).toOption.get
    def roundTrip = Decoder.fromSchema(schema).read(output).toOption.get

    assert(output == expected)
    assert(roundTrip == data)
  }

  "schema" should {
    "encode/decode a product" in {
      val role = Role("admin", User(203, "tim"))
      val schema: Schema[Role] = Schema.record { field =>
        (
          field("capability", _.capability)(Schema.str),
          field("user", _.user) { // nesting
            Schema.record { field =>
              (
                field("id", _.id)(Schema.num),
                field("firstName", _.name)(Schema.str) // renaming
              ).mapN(User.apply)
            }
          }
        ).mapN(Role.apply)
      }
      val expected = Value.m(
        Name("capability") -> Value.s(role.capability),
        Name("user") -> Value.m(
          Name("id") -> Value.n(role.user.id),
          Name("firstName") -> Value.s(role.user.name)
        )
      )

      test(schema, role, expected)
    }

    "encode/decode a product including additional structure" in {
      val user = User(203, "tim")
      val schema = Schema.record[User] { field =>
        (
          field("id", _.id)(Schema.num),
          field("name", _.name)(Schema.str)
        ).mapN(User.apply)
      }
      val versionedSchema: Schema[User] = Schema.record[User] { field =>
        field.const("version", "1.0")(Schema.str) *>
          field("payload", x => x)(schema)
      }
      val expected = Value.m(
        Name("version") -> Value.s("1.0"),
        Name("payload") -> Value.m(
          Name("id") -> Value.n(user.id),
          Name("name") -> Value.s(user.name)
        )
      )

      test(versionedSchema, user, expected)
    }

    "encode/decode a product with more than 22 fields" in {
      val big = Big(
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f",
        "f"
      )

      val bigSchema = Schema.record[Big](
        field =>
          for {
            a <- field("1", _.one)(Schema.str)
            b <- field("2", _.two)(Schema.str)
            c <- field("3", _.three)(Schema.str)
            d <- field("4", _.four)(Schema.str)
            e <- field("5", _.five)(Schema.str)
            f <- field("6", _.six)(Schema.str)
            g <- field("7", _.seven)(Schema.str)
            h <- field("8", _.eight)(Schema.str)
            i <- field("9", _.nine)(Schema.str)
            j <- field("10", _.ten)(Schema.str)
            k <- field("11", _.eleven)(Schema.str)
            l <- field("12", _.twelve)(Schema.str)
            m <- field("13", _.thirteen)(Schema.str)
            n <- field("14", _.fourteen)(Schema.str)
            o <- field("15", _.fifteen)(Schema.str)
            p <- field("16", _.sixteen)(Schema.str)
            q <- field("17", _.seventeen)(Schema.str)
            r <- field("18", _.eighteen)(Schema.str)
            s <- field("19", _.nineteen)(Schema.str)
            t <- field("20", _.twenty)(Schema.str)
            u <- field("21", _.twentyOne)(Schema.str)
            v <- field("22", _.twentyTwo)(Schema.str)
            w <- field("23", _.twentyThree)(Schema.str)
          } yield
            Big(
              a,
              b,
              c,
              d,
              e,
              f,
              g,
              h,
              i,
              j,
              k,
              l,
              m,
              n,
              o,
              p,
              q,
              r,
              s,
              t,
              u,
              v,
              w
            )
      )

      val expected = Value.m(
        Name("1") -> Value.s(big.one),
        Name("2") -> Value.s(big.two),
        Name("3") -> Value.s(big.three),
        Name("4") -> Value.s(big.four),
        Name("5") -> Value.s(big.five),
        Name("6") -> Value.s(big.six),
        Name("7") -> Value.s(big.seven),
        Name("8") -> Value.s(big.eight),
        Name("9") -> Value.s(big.nine),
        Name("10") -> Value.s(big.ten),
        Name("11") -> Value.s(big.eleven),
        Name("12") -> Value.s(big.twelve),
        Name("13") -> Value.s(big.thirteen),
        Name("14") -> Value.s(big.fourteen),
        Name("15") -> Value.s(big.fifteen),
        Name("16") -> Value.s(big.sixteen),
        Name("17") -> Value.s(big.seventeen),
        Name("18") -> Value.s(big.eighteen),
        Name("19") -> Value.s(big.nineteen),
        Name("20") -> Value.s(big.twenty),
        Name("21") -> Value.s(big.twentyOne),
        Name("22") -> Value.s(big.twentyTwo),
        Name("23") -> Value.s(big.twentyThree)
      )

      test(bigSchema, big, expected)
    }

    "encode a newtype with no wrapping" in {
      val token = TraceToken("1234")
      val schema = Schema.str.imap(TraceToken.apply)(_.value)
      val expected = Value.s(token.value)

      test(schema, token, expected)
    }

    "encode/decode enums" in {
      val started = Event(Started, "transaction started event")
      val completed = Event(Completed, "transaction completed event")

      // Similar to the one provided by enumeratum
      def parser: String => Option[EventType] = {
        case "Started" => Started.some
        case "Completed" => Completed.some
        case _ => none
      }

      val stateSchema: Schema[EventType] = Schema.str.imapErr { s =>
        parser(s) toRight ReadError()
      }(_.toString)

      val eventSchema: Schema[Event] = Schema.record { field =>
        (
          field("type", _.state)(stateSchema),
          field("value", _.value)(Schema.str)
        ).mapN(Event.apply)
      }

      val expectedStarted = Value.m(
        Name("type") -> Value.s("Started"),
        Name("value") -> Value.s(started.value)
      )

      val expectedCompleted = Value.m(
        Name("type") -> Value.s("Completed"),
        Name("value") -> Value.s(completed.value)
      )

      test(eventSchema, started, expectedStarted)
      test(eventSchema, completed, expectedCompleted)
    }

    "encode/decode an ADT using a discriminator key" in {
      val schema: Schema[Upload] = {
        val error = Schema
          .record[Error] { field =>
            (
              field("msg", _.msg)(Schema.str),
              field("cause", _.cause)(Schema.str)
            ).mapN(Error.apply)
          }
          .tag("error")

        val warning = Schema
          .record[Warning] { field =>
            (
              field("msg", _.msg)(Schema.str),
              field("cause", _.cause)(Schema.str)
            ).mapN(Warning.apply)
          }
          .tag("warning")

        val unknown: Schema[Unknown.type] =
          Schema.unit.tag("unknown").imap(_ => Unknown)(_ => ())

        val successful = Schema
          .record[Successful] { field =>
            (
              field("link", _.link)(Schema.str),
              field("expires", _.expires)(Schema.num)
            ).mapN(Successful.apply)
          }
          .tag("successful")

        Schema.record[Upload] { field =>
          (
            field("id", _.id)(Schema.str),
            field("status", _.status) {
              Schema.oneOf { alt =>
                alt(error) |+| alt(warning) |+| alt(unknown) |+| alt(successful)
              }
            }
          ).mapN(Upload.apply)
        }
      }

      val error = Error("error msg", "error cause")
      val errorUp = Upload("error id", error)
      val warning = Warning("warning msg", "warning cause")
      val warningUp = Upload("warning id", warning)
      val unknownUp = Upload("unknown id", Unknown)
      val successful = Successful("link", 150)
      val successfulUp = Upload("successful id", successful)

      val expectedError = Value.m(
        Name("id") -> Value.s(errorUp.id),
        Name("status") -> Value.m(
          Name("error") -> Value.m(
            Name("msg") -> Value.s(error.msg),
            Name("cause") -> Value.s(error.cause)
          )
        )
      )
      val expectedWarning = Value.m(
        Name("id") -> Value.s(warningUp.id),
        Name("status") -> Value.m(
          Name("warning") -> Value.m(
            Name("msg") -> Value.s(warning.msg),
            Name("cause") -> Value.s(warning.cause)
          )
        )
      )
      val expectedUnknown = Value.m(
        Name("id") -> Value.s(unknownUp.id),
        Name("status") -> Value.m(
          Name("unknown") -> Value.m()
        )
      )
      val expectedSuccessful = Value.m(
        Name("id") -> Value.s(successfulUp.id),
        Name("status") -> Value.m(
          Name("successful") -> Value.m(
            Name("link") -> Value.s(successful.link),
            Name("expires") -> Value.n(successful.expires)
          )
        )
      )

      test(schema, errorUp, expectedError)
      test(schema, warningUp, expectedWarning)
      test(schema, unknownUp, expectedUnknown)
      test(schema, successfulUp, expectedSuccessful)
    }

    "encode/decode an ADT using a discriminator field" ignore {
      ???
    }

    // "encode/decode nested ADTs using a discriminator" in {
    //   val user = User(203, "tim")
    //   val role = Role("admin", user)
    //   val error = Error("MyError")
    //   val auth = Auth(role, 1)

    //   val userSchema: Schema[User] = record[User] { field =>
    //     (
    //       field("id", _.id)(num),
    //       field("name", _.name)(str)
    //     ).mapN(User.apply)
    //   }
    //   val roleSchema: Schema[Role] = record[Role] { field =>
    //     (
    //       field("capability", _.capability)(str),
    //       field("user", _.user)(userSchema)
    //     ).mapN(Role.apply)
    //   }
    //   val statusSchema: Schema[Status] = Schema.oneOf[Status] { alt =>
    //     val errorSchema = record[Error] { field =>
    //       field("message", _.message)(str).map(Error.apply)
    //     }

    //     val authSchema = record[Auth] { field =>
    //       (
    //         field("role", _.role)(roleSchema),
    //         field("token", _.token)(num)
    //       ).mapN(Auth.apply)
    //     }

    //     alt(errorSchema tag "error") |+| alt(authSchema tag "auth")
    //   }

    //   val expectedError = Value.m(
    //     Name("error") -> Value.m(
    //       Name("message") -> Value.s(error.message)
    //     )
    //   )
    //   val expectedAuth = Value.m(
    //     Name("auth") -> Value.m(
    //       Name("role") -> Value.m(
    //         Name("capability") -> Value.s(role.capability),
    //         Name("user") -> Value.m(
    //           Name("id") -> Value.n(role.user.id),
    //           Name("name") -> Value.s(role.user.name)
    //         )
    //       ),
    //       Name("token") -> Value.n(auth.token)
    //     )
    //   )

    //   test(statusSchema, error, expectedError)
    //   test(statusSchema, auth, expectedAuth)
    // }

    // """encode/decode ADTs using an embedded "type" field""" in {
    //   // TODO remove the tagging here, a bit more orthogonal testing
    //   val user = User(203, "tim")
    //   val one = One(user)
    //   val two = Two(user)

    //   val userSchema: Schema[User] = record { field =>
    //     (
    //       field("id", _.id)(num),
    //       field("name", _.name)(str)
    //     ).mapN(User.apply)
    //   }

    //   val sameSchema: Schema[Same] = Schema.oneOf { alt =>
    //     val oneSchema = record[One] { field =>
    //       field.const("type", "one")(str) *>
    //         field("user", _.user)(userSchema).map(One.apply)
    //     }

    //     val twoSchema = record[Two] { field =>
    //       field.const("type", "two")(str) *>
    //         field("user", _.user)(userSchema).map(Two.apply)
    //     }

    //     alt(oneSchema) |+| alt(twoSchema)
    //   }

    //   val expectedOne = Value.m(
    //     Name("type") -> Value.s("one"),
    //     Name("user") -> Value.m(
    //       Name("id") -> Value.n(one.user.id),
    //       Name("name") -> Value.s(one.user.name)
    //     )
    //   )

    //   val expectedTwo = Value.m(
    //     Name("type") -> Value.s("two"),
    //     Name("user") -> Value.m(
    //       Name("id") -> Value.n(one.user.id),
    //       Name("name") -> Value.s(one.user.name)
    //     )
    //   )

    //   test(sameSchema, one, expectedOne)
    //   test(sameSchema, two, expectedTwo)
    // }

    // "encode/decode objects as empty records (e.g. for use in mixed ADTs)" in {
    //   // TODO rename to encode decode objects as empty records or Strings, remove const
    //   // revamp ADT tests altogether
    //   val openDoor = Door(Open)
    //   val closedDoor = Door(Closed)

    //   val stateSchema: Schema[State] = {
    //     val openSchema =
    //       unit.tag("open").imap(_ => Open)(_ => ())
    //     val closedSchema = unit.tag("closed").imap(_ => Open)(_ => ())

    //     Schema.oneOf[State] { alt =>
    //       alt(openSchema) |+| alt(closedSchema)
    //     }
    //   }

    //   val doorSchema = record[Door] { field =>
    //     field("state", _.state)(stateSchema).map(Door.apply)
    //   }

    //   val expectedOpen = Value.m(
    //     Name("state") -> Value.m(
    //       Name("open") -> Value.m()
    //     )
    //   )
    //   val expectedClosed = Value.m(
    //     Name("state") -> Value.m(
    //       Name("closed") -> Value.m()
    //     )
    //   )

    //   test(doorSchema, openDoor, expectedOpen)
    //   test(doorSchema, closedDoor, expectedClosed)
    // }

    // "encode/decode objects as strings" in {
    //   // TODO merge with the above
    //   val openDoor = Door(Open)
    //   val closedDoor = Door(Closed)
    //   val state = Schema.oneOf[State] { alt =>
    //     alt { str.const("open", Open) } |+| alt {
    //       str.const("closed", Closed)
    //     }
    //   }

    //   val doorSchema: Schema[Door] = record { field =>
    //     field("state", _.state)(state).map(Door.apply)
    //   }
    //   val expectedOpen = Value.m(
    //     Name("state") -> Value.s("open")
    //   )
    //   val expectedClosed = Value.m(
    //     Name("state") -> Value.s("closed")
    //   )

    //   test(doorSchema, openDoor, expectedOpen)
    //   test(doorSchema, closedDoor, expectedClosed)
    // }

  }

  val compileTimeInferenceSpec = {
    val userSchema: Schema[User] = Schema.record { field =>
      (
        field("id", _.id)(Schema.num),
        field("name", _.name)(Schema.str)
      ).mapN(User.apply)
    }

    val userSchema2 = Schema.record[User] { field =>
      (
        field("id", _.id)(Schema.num),
        field("name", _.name)(Schema.str)
      ).mapN(User.apply)
    }

    // random impl but it does not matter
    def completedSchema: Schema[Completed.type] =
      Schema.record(_("foo", _.toString)(Schema.str).as(Completed))
    def startedSchema: Schema[Started.type] =
      Schema.record(_("foo", _.toString)(Schema.str).as(Started))

    val eventTypeSchema: Schema[EventType] = Schema.oneOf { alt =>
      alt(completedSchema) |+| alt(startedSchema)
    }

    val eventTypeSchema2 = Schema.oneOf[EventType] { alt =>
      alt(completedSchema) |+| alt(startedSchema)
    }

    val eventTypeSchema3 = Schema.oneOf[EventType] { alt =>
      implicit val p1 = Prism.derive[EventType, Started.type]
      val p2 = Prism.derive[EventType, Completed.type]

      alt(startedSchema) |+| alt(completedSchema)(p2)
    }

    val traceTokenSchema = Schema.str.imap(TraceToken.apply)(_.value)

    val (_, _, _, _, _, _) =
      (
        userSchema,
        userSchema2,
        eventTypeSchema,
        eventTypeSchema2,
        eventTypeSchema3,
        traceTokenSchema
      )
  }
}
