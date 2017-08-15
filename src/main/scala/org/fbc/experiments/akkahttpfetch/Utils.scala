/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fbc.experiments.akkahttpfetch

import java.util.Calendar

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.concurrent.duration.Duration

trait Utils extends StrictLogging {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val executionContext: ExecutionContextExecutor

  def performSequentially[A](items: Seq[A])(f: A => Future[Unit]): Future[Unit] = {
    items.headOption match {
      case Some(nextItem) =>
        val fut = f(nextItem)
        fut.flatMap { _ =>
          performSequentially(items.tail)(f)
        }
      case None =>
        Future.successful(())
    }
  }

  def harakiri2(agony: Int = 5): Unit = {
    val fs = (1 to agony)
    logger.info(s"awaiting to die ${Calendar.getInstance().getTime()}")
    Await.ready(performSequentially(fs) { i => Future { Thread.sleep(1000); logger.info("hello " + i) }}, Duration.Inf)
    logger.info(s"done waiting ${Calendar.getInstance().getTime()}")
    system.terminate()
  }

  def harakiri(agony: Int = 5): Unit = {
    val fs = (1 to agony).map { i =>
      Future {
        Thread.sleep(1000)
        logger.info("hello " + i)
        i
      }
    }
    val f = Future.sequence(fs)
    logger.info(s"awaiting to die ${Calendar.getInstance().getTime()}")
    Await.result(f, Duration.Inf)
    logger.info(s"done waiting ${Calendar.getInstance().getTime()}")
    system.terminate()
  }
}
