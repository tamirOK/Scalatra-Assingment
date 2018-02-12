package com.example.app


import java.util.{Calendar, Date}
import javax.servlet.http.HttpServletRequest

import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import scala.collection.mutable
import authentikat.jwt._


class MyScalatraServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  case class Tweet(ownerId: Int, tweetId: Int, tweetBody: String, createdAt: Date)
  case class User(id: Int, email: String, login: String, password: String)

  var userDB = List[User]()
  val tokens = mutable.Map[String, String]()
  private var tweets = List[Tweet]()
  private var tweetId = 0
  private var userId_ = 0
  private val subscriptions = mutable.Map[Int, mutable.ListBuffer[Int]]()

  before() {
    contentType = formats("json")
  }

  after() {
    contentType = formats("json")
  }

  private def validateToken(request: HttpServletRequest): (Boolean, String, Option[Map[String, String]]) = {
    val token = request.getHeader("Authorization").split(" ").toList(1)
    val isValid = JsonWebToken.validate(token, "secretkey")
    if (!isValid)
      (false, "", None)
    else {
      val claims: Option[Map[String, String]] = token match {
        case JsonWebToken(header, claimsSet, signature) =>
          claimsSet.asSimpleMap.toOption
        case x =>
          None
      }
      println(claims)
      (true, token, claims)
    }
  }

  post("/register/") {
    val parsedData = parse(request.body).extract[Map[String, String]]
    val (email, login, password) = (parsedData("email"), parsedData("login"), parsedData("password"))

    // TODO: Add password hashing
    val newUser = User(userId_, email, login, password)

    // check if user id and email are unique
    if (userDB.exists(user => user.id == newUser.id || user.email == newUser.email))
      Map("result" -> Conflict("Username with such id or email already exists"))
    else {
      // add new user
      userDB = userDB :+ newUser
      userId_ += 1
      Map("result" -> Ok("Successfully registered"))
    }
  }

  post("/login/") {
    val parsedData = parse(request.body).extract[Map[String, String]]
    val login = parsedData("login")
    val password = parsedData("password")

    // check if login and password exist in db
    if (userDB.exists(user => user.login == login && user.password == password)) {
      val user = userDB.filter(user => user.login == login && user.password == password).head
      val header = JwtHeader("HS256")
      val claimsSet = JwtClaimsSet(Map("login" -> user.login, "id" -> user.id))
      val jwt: String = JsonWebToken(header, claimsSet, "secretkey")

      // send JWT
      Map("token" -> jwt)
    }
    else
      Map("result" -> Conflict("Incorrect login or password"))

  }


  post("/create_tweet/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetBody = parsedData("tweetBody")
      val ownerId = claimsMap.get("id").toInt

      println(claimsMap)

      tweets = tweets :+ Tweet(ownerId, tweetId, tweetBody, Calendar.getInstance.getTime) // add new tweet
      tweetId += 1 // increase tweetId

      // return result
      Map(
        "status" -> "Tweet successfully added",
        "tweet_id" -> (tweetId - 1).toString
      )
    }
  }

  put("/edit_tweet/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetID = parsedData("tweetId").toInt
      val newTweetBody = parsedData("newTweetBody")
      val ownerId = claimsMap.get("id").toInt

      tweets = tweets.filter(_.tweetId != tweetID)
      tweets = tweets :+ Tweet(ownerId, tweetID, newTweetBody, Calendar.getInstance.getTime)

      Map("result" -> Ok("Tweet successfully edited"))
    }
  }

  delete("/remove_tweet/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetID = parsedData("tweetId")
      val ownerId = claimsMap.get("id")

      // get tweet with provided tweetId
      if (tweets.filter(_.tweetId == tweetID.toInt).head.ownerId != ownerId)
        Forbidden("Tweet deletion is not allowed")
      else
        tweets = tweets.filter(_.tweetId != tweetID.toInt) // remove tweet with provided tweetId
        Map("result" -> Ok("Tweet successfully removed"))
    }
  }

  get("/subscribe/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val userId = parsedData("userId").toInt // to whom subscribe
      val currentUserId = claimsMap.get("id").toInt

      subscriptions(userId) += userId
      Map("result" -> Ok("Successfully subscribed"))
    }
  }

  get("/feed/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val currentUserId = claimsMap.get("id").toInt

      for (userId <- subscriptions(currentUserId);
                        tweet <- tweets.reverse
                        if tweet.ownerId == userId) yield tweet

    }
  }

  get("/feed/:user_id/") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val interestedUserId = params("user_id").toInt

      for (userId <- subscriptions(interestedUserId);
           tweet <- tweets.reverse
           if tweet.ownerId == userId) yield tweet

    }
  }
}
