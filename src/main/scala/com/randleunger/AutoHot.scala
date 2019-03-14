/*
 * Copyright (c) 2019 - Randy Unger. All Rights Reserved.
 */

package com.randleunger

/**
  *
  * Created by Randy Unger
  *
  **/


import akka.actor.ActorSystem
import org.ehcache.CacheManagerBuilder
import org.ehcache.config.CacheConfigurationBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/*
  Utility for succinctly marking a piece of code to be executed, cached, and refreshed at
  predefined intervals. This style of caching essentially caches not just the results of
  executing a segment of code but also caches the code itself (albeit in different places).
  This makes it very lightweight to write code that periodically checks an API, or a database
  value without having to manage a lot of infrastructure, or hammering the data source.
*/
//todo: pass in ehcache config
//todo: Optionally fail to update based on function comparing prev value to new value (check for errors)
class AutoHot(logger: Logger)(implicit executionContext: ExecutionContext) {

  //todo pass in Test Akka system
  implicit val akkaSystem = ActorSystem("AutoHotSystem")

  val cacheName = "AutoHotCache"

  //Bypass ehCache type management, since AutoHot can manage this more simply due type information in closure
  val keyClass = classOf[String]
  val valueClass = classOf[Any] //Any is OK since we have [T] in closure at unwrap time! How convenient!

  private val cache = {
    logger.info("Building new cache manager & cache")
    val cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
      .withCache(cacheName,
        CacheConfigurationBuilder.newCacheConfigurationBuilder()
          .buildConfig(keyClass, valueClass))
      .build(true)

    val newCache = cacheManager.getCache(cacheName, keyClass, valueClass)
    newCache
  }

  /*
  Wrap the code you want to be periodically executed in a call to this method.

  @param label An identifier for the factory, used as a key. Must be unique.
  @param duration A FiniteDuration indicating how often the factory should be re-executed
  @param tFactory A chunk of code returning a T
   */
  def heat[T](label: String, duration: FiniteDuration)(tFactory: => T): T = {

    //Check cache for most recent value
    val existingValue = {
      Option(cache.get(label))
    }

    //If value not present in cache, create value from factory
    existingValue match {
      case Some(value) => {
        logger.info(s"Using cached value for $label")
        value.asInstanceOf[T]
      }
      case None => {

        //Add logging and caching to the supplied factory
        def loggingFactory: () => T = () => {
          logger.info(s"AutoHot: Executing $label")

          //Execute the supplied factory
          //todo: Time and log the execution
          val t = tFactory

          //Store result of factory execution in cache
          //todo: execute validation function if supplied
          cache.put(label, t)

          //Schedule the next instance of factory execution
          akkaSystem.scheduler.scheduleOnce(duration){
            loggingFactory()
          }

          //Return the generated object
          t
        }

        //todo: Add cancellable to cache, for sched manipulation
        //        val cancellable = akkaSystem.scheduler.schedule(duration, duration)(loggingFactory())

        //Execute factory to get the return value
        val initialT = loggingFactory()
        initialT

      }
    }
  }
}


trait Logger {
  def info(msg: String)
}

object PrintLogger extends Logger {
  override def info(msg: String): Unit = println(s"Info: $msg")
}

object AutoHot {
  val prod = new AutoHot(PrintLogger)(scala.concurrent.ExecutionContext.Implicits.global)
  def apply() = prod
}

