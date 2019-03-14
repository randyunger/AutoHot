package com.randleunger

/*
 * Copyright (c) 2016 - Randy Unger. All Rights Reserved.
 */

/**
  *
  * Created by Unger
  *
  **/

import org.scalatest.FunSuite
import scala.concurrent.duration._

class AutoHotTest extends FunSuite {

  test("returns the results of the factory") {
    val fortyTwo = AutoHot().heat("ReturnsResultOfThunk", 1.seconds) {
      println("Here's my factory!")
      42
    }
    assert(42 == fortyTwo)
  }

  test("invokes the factory only once") {
    var runs = 0
    def addOne = AutoHot().heat("OnlyOnce", 1.minute) {
      runs = runs + 1
      runs
    }
    addOne
    addOne
    addOne
    assert(1 == runs)
  }

  test("reference types") {
    case class Something(a: String)
    def go = AutoHot().heat("RefTest", 1.second) {
      Something("str")
    }
    val actual = go
    assert("str" == actual.a)

  }

  test("different types") {

  }

  //If your factory throws an exception, you should probably handle that in the factory.
  //Here we see that an exception in the factory
  test("Factory throws exception, can be caught at usage site") {
    val expected = try {
      AutoHot().heat("Exception Test", 1.second) {
        throw new Exception("Oops!")
      }
    } catch {
      case ex: Throwable => true
    }

    assert( expected == true)
  }

  test("Factory throws exception on second call") {
    var runs = 0

    def go() = {
      AutoHot().heat("Exception in 2nd call", 10.millis) {
        val x = try {
          runs match {
            case 0 => {
              runs = runs + 1
              "a"
            }
            case _ =>{
              throw new Exception("Oops!")
              "b"
            }
          }
        } catch {
          case _ => {
            println("Caught my own exception")
            "c"
          }
        }
        x
      }
    }

    val first = go()
    Thread.sleep(100)
    val second = go()

    assert(first == "a")
    assert(second == "c")
  }

  test("Nested types"){
    val toStore = Option(List(Option()))

    def go() = AutoHot().heat("Nested Types", 1.second) {
      toStore
    }
    val expected = go()
    assert(toStore == expected)
  }

  //Test that values are updated on subsequent calls
  test("Values are updated on subsequent calls"){
    def go() = AutoHot().heat("Updated subsequently", 10 millis) {
      System.currentTimeMillis()
    }
    val warmup = go()
    //Sleep for more than Duration so that we're retrieving from cache instead of retrieving the initial result
    Thread.sleep(1000)
    val expected = go()

    assert(expected - warmup > 500)
  }

  //Test that AutoHot can store and retrieve types with multiple Type params
  test("multiple type params"){
    case class ABC[A,B,C](a:A, b:B, c:C)

    def go() = AutoHot().heat("Multi Types", 10 millis) {
      ABC(1, "Hi", Nil)
    }
    val warmup = go()
    //Sleep for more than Duration so that we're retrieving from cache instead of retrieving the initial result
    Thread.sleep(1000)
    val expected = go()

    assert(ABC(1, "Hi", Nil) == expected)
  }

  //This test sucks since its highly variable in how many loops execute
  //Test that AutoHot repeats
  test("testHeat repeat") {
    val delay = 100
    val expectedRuns = 10
    var actualRuns = 0
    val countRuns = AutoHot().heat("Repeat", delay.millis) {
      println("running the factory!")
      actualRuns = actualRuns + 1
    }
    Thread.sleep(delay * expectedRuns)
    //    Thread.sleep(10000)
    println(s"Expected runs: $expectedRuns, actual runs: $actualRuns")
    assert(expectedRuns - actualRuns < 3) //Can be off by one or two, but not more than that
  }

  //test what happens if the same label is used multiple times
}
