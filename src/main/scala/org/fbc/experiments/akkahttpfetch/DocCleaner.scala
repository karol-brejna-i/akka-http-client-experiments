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

import com.typesafe.scalalogging.StrictLogging
import org.htmlcleaner.{CleanerProperties, HtmlCleaner, PrettyXmlSerializer, TagNode}

import scala.xml.{Elem, XML}

trait DocCleaner extends StrictLogging {

  def getHtmlCleaner(htmlString: String): TagNode = {
    val cleaner = new HtmlCleaner()
    cleaner.getProperties().setPruneTags("script,style")

    val tagNode = cleaner.clean(htmlString)
    logger.debug(s"tagnode {}", tagNode)
    tagNode
  }

  def getXML(doc: String): Elem = {
    val cleanedDoc = getHtmlCleaner(doc)

    val cleanedHtml = new PrettyXmlSerializer(new CleanerProperties()).getAsString(cleanedDoc)
    val docXml = XML.loadString(cleanedHtml)
    docXml
  }

}
