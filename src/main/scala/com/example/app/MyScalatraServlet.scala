package com.example.app


import java.util.{Calendar, Date}
import javax.servlet.http.HttpServletRequest

import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport

import scala.collection.mutable
import authentikat.jwt._


sealed trait TweetT{def ownerId: Int; def tweetId: Int}
case class Tweet(override val ownerId: Int, override val tweetId: Int, tweetBody: String, createdAt: Date) extends TweetT
case class ReTweet(ownerId: Int, tweetId: Int, otweetId: Int, tweetBody: String, createdAt: Date) extends TweetT

class MyScalatraServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats
  case class User(id: Int, email: String, login: String, password: String)

  var userDB = List[User]()
  private var tweets = List[TweetT]()
  private var tweetId_ = 0
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
    if (isValid){
      val claims: Option[Map[String, String]] = token match {
        case JsonWebToken(header, claimsSet, signature) =>
          claimsSet.asSimpleMap.toOption
        case x =>
          None
      }
      println(claims)
      if (userDB.exists(user => user.id == claims.get("id").toInt))
        return (true, token, claims)
    }
    (false, "", None)
  }

  post("/register/?") {
    val parsedData = parse(request.body).extract[Map[String, String]]
    val (email, login, password) = (parsedData("email"), parsedData("login"), parsedData("password"))

    // TODO: Add password hashing
    //MessageDigest.getInstance("SHA-256").digest(password.getBytes("UTF-8"))
    val newUser = User(userId_, email, login, password)

    // check if user id and email are unique
    if (userDB.exists(user => user.id == newUser.id || user.email == newUser.email))
      Map("result" -> Conflict("Username with such id or email already exists"))
    else {
      // add new user
      userDB = userDB :+ newUser
      subscriptions.update(userId_, mutable.ListBuffer.empty[Int])
      userId_ += 1
      Map("result" -> Ok("Successfully registered"))
    }
  }

  post("/login/?") {
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


  post("/create_tweet/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetBody = parsedData("tweetBody")
      val ownerId = claimsMap.get("id").toInt

      println(claimsMap)

      tweets = tweets :+ Tweet(ownerId, tweetId_, tweetBody, Calendar.getInstance.getTime) // add new tweet
      tweetId_ += 1 // increase tweetId_

      // return result
      Map(
        "status" -> "Tweet successfully added",
        "tweet_id" -> (tweetId_ - 1).toString
      )
    }
  }

  post("/create_retweet/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetId = parsedData("tweetId").toInt
      val retweetBody = parsedData("retweetBody")
      val ownerId = claimsMap.get("id").toInt

      println(claimsMap)

      if(tweets.exists(_.tweetId == tweetId)) {
        val otweetId = tweets.find(tweet => tweet.tweetId == tweetId) match {
          case Some(Tweet(_, tmpTweetId, _, _)) => tmpTweetId
          case Some(ReTweet(_, _, tmpTweetId, _, _)) => tmpTweetId
        }
        tweets = tweets :+ ReTweet(ownerId, tweetId_, otweetId, retweetBody, Calendar.getInstance.getTime) // add new retweet
        tweetId_ += 1 // increase tweetId
        Map(
          "status" -> "Retweet successfully added",
          "tweet_id" -> (tweetId_ - 1).toString
        )
      }else
        Map("status" -> Conflict("Original message not found"))
      // return result
    }
  }

  put("/edit_tweet/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetID = parsedData("tweetId").toInt
      val newTweetBody = parsedData("newTweetBody")
      val ownerId = claimsMap.get("id").toInt

      if (tweets.filter(_.tweetId == tweetID).head.ownerId != ownerId)
        Map("result" -> Forbidden("Tweet edition is not allowed"))
      else {
        tweets = tweets.filter(_.tweetId != tweetID)
        tweets = tweets :+ Tweet(ownerId, tweetID, newTweetBody, Calendar.getInstance.getTime)
        Map("result" -> Ok("Tweet successfully edited"))
      }
    }
  }

  delete("/remove_tweet/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val tweetID = parsedData("tweetId").toInt
      val ownerId = claimsMap.get("id").toInt

      // get tweet with provided tweetId
      if (tweets.filter(_.tweetId == tweetID).head.ownerId != ownerId)
        Map("result" -> Forbidden("Tweet deletion is not allowed"))
      else {
        tweets = tweets.filter(tweet => tweet.tweetId != tweetID && (tweet match {case ret: ReTweet => ret.otweetId != tweetID case _ => true})) // remove tweet with provided tweetId
        Map("result" -> Ok("Tweet successfully removed"))
      }
    }
  }

  post("/subscribe/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val parsedData = parse(request.body).extract[Map[String, String]]
      val userId = parsedData("userId").toInt // to whom subscribe
      val currentUserId = claimsMap.get("id").toInt

      userId match {
        case x if x == currentUserId => Map("result" -> Conflict("Can't self subscribe"))
        case _ if subscriptions(currentUserId).contains(userId) => Map("result" -> Conflict("Already subscribed"))
        case _ if userId < userId_ & userId >= 0 => subscriptions(currentUserId) += userId
          Map("result" -> Ok("Successfully subscribed"))
        case _ => Map("result" -> Conflict("Wrong user id"))
      }
    }
  }

  get("/feed/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val currentUserId = claimsMap.get("id").toInt

      for (userId <- subscriptions(currentUserId);
                        tweet <- tweets.reverse
                        if tweet.ownerId == userId) yield tweet

    }
  }

  get("/feed/:user_id/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val interestedUserId = params("user_id").toInt

      for (userId <- subscriptions(interestedUserId);
           tweet <- tweets.reverse
           if tweet.ownerId == userId) yield tweet

    }
  }

  get("/tweets/:user_id/?") {
    val (isValid, token, claimsMap) = validateToken(request)

    if (!isValid)
      Map("result" -> Unauthorized("Wrong token"))

    else {
      val interestedUserId = params("user_id").toInt
      for (tweet <- tweets.reverse if tweet.ownerId == interestedUserId) yield tweet
    }
  }


}
