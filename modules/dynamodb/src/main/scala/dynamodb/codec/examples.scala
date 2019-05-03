/*
 * Copyright 2018 OVO Energy
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

import com.ovoenergy.comms.aws.dynamodb.model._

import cats._, implicits._

import Schema._

object examples {
  case class User(id: Int, name: String)
  case class Role(capability: String, u: User)

  def userSchema: Schema[User] = rec(
    (
      field[User]("id", num, _.id),
      field[User]("name", str, _.name)
    ).mapN(User.apply)
  )

  def roleSchema: Schema[Role] = {
    def capability: Role => String = _.capability

    rec(
      (
        field("capability", str, capability),
        field[Role]("user", userSchema, _.u)
      ).mapN(Role.apply)
    )
  }

  def role = Role("admin", User(20, "joe"))

  def a = Encoder.fromSchema(roleSchema).write(role)
  def b = Decoder.fromSchema(roleSchema).read(a)

// scala> a
// res0: com.ovoenergy.comms.aws.dynamodb.model.AttributeValue = M(Map(AttributeName(capability) -> S(admin), AttributeName(user) -> M(Map(AttributeName(id) -> N(20), AttributeName(name) -> S(joe)))))

// scala> b
// res1: Either[ParseError,examples.Role] = Right(Role(admin,User(20,joe)))

  sealed trait Status
  case class Error(s: String) extends Status
  case class Auth(r: Role) extends Status

  val statusSchema: Schema[Status] = oneOf {
    List(
      alt[Status, String]("error", str)(Error.apply) {
        case Error(v) => v.some
        case _ => None
      },
      alt[Status, Role]("auth", roleSchema)(Auth.apply) {
        case Auth(v) => v.some
        case _ => None
      }
    )
  }

  val c = Encoder.fromSchema(statusSchema).write(Error("MyError"))
  val d = Encoder.fromSchema(statusSchema).write(Auth(role))
}
