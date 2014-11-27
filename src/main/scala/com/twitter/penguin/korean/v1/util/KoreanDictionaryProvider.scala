/*
 * Twitter Korean Text - Scala library to process Korean text
 *
 * Copyright 2014 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.penguin.korean.v1.util

import java.io.{FileInputStream, InputStream}
import java.util.zip.GZIPInputStream

import com.twitter.penguin.korean.util.CharArraySet
import com.twitter.penguin.korean.v1.util.KoreanConjugation._
import com.twitter.penguin.korean.v1.util.KoreanPos._
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Provides a singleton Korean dictionary
 */
object KoreanDictionaryProvider {
  private[this] def readStreamByLine(stream: InputStream, filename: String): Iterator[String] = {
    require(stream != null, "Resource not loaded: " + filename)
    Source.fromInputStream(stream)(io.Codec("UTF-8"))
        .getLines()
        .map(_.trim)
        .filter(_.length > 0)
  }

  private[this] def readWordFreqs(filename: String): collection.mutable.Map[CharSequence, Float] = {
    var freqMap: collection.mutable.Map[CharSequence, Float] =
      new java.util.HashMap[CharSequence, Float]

    readFileByLineFromResources(filename).foreach {
      case line => if (line.contains("\t")) {
        val data = line.split("\t")
        freqMap += (data(0) -> data(1).slice(0, 6).toFloat)
      }
    }
    freqMap
  }

  private[this] def readWordMap(filename: String): Map[String, String] = {
    readFileByLineFromResources(filename).filter {
      case line: String => line.contains(" ")
    }.map {
      case line =>
        val data = line.split(" ")
        (data(0), data(1))
    }.toMap
  }


  protected[korean] def readWordsAsSeq(filename: String): Seq[String] = {
    readFileByLineFromResources(filename).toSeq
  }

  protected[korean] def readWordsAsSet(filenames: String*): Set[String] = {
    filenames.foldLeft(Set[String]()) {
      case (output: Set[String], filename: String) =>
        output.union(
          readFileByLineFromResources(filename).toSet
        )
    }
  }

  protected[korean] def readWords(filenames: String*): CharArraySet = {
    val set = newCharArraySet
    filenames.foreach(
      filename => readFileByLineFromResources(filename).foreach(set.add)
    )
    set
  }

  protected[korean] def readFileByLineFromResources(filename: String): Iterator[String] = {
    readStreamByLine(
      if (filename.endsWith(".gz")) {
        new GZIPInputStream(getClass.getResourceAsStream(filename))
      } else {
        getClass.getResourceAsStream(filename)
      }
      , filename
    )
  }

  protected[korean] def readGzipTBininaryFromFile(filename: String): TBinaryProtocol = {
    val in = new GZIPInputStream(new FileInputStream(filename))
    new TBinaryProtocol(new TIOStreamTransport(in))
  }

  protected[korean] def readGzipTBininaryFromResource(filename: String): TBinaryProtocol = {
    val in = new GZIPInputStream(getClass.getResourceAsStream(filename))
    new TBinaryProtocol(new TIOStreamTransport(in))
  }


  protected[korean] def newCharArraySet: CharArraySet = {
    new CharArraySet(10000, false)
  }

  lazy val koreanEntityFreq: collection.mutable.Map[CharSequence, Float] =
    readWordFreqs("freq/entity-freq.txt.gz")

  lazy val koreanDictionary: collection.mutable.Map[KoreanPos, CharArraySet] = {
    var map: collection.mutable.Map[KoreanPos, CharArraySet] =
      new java.util.HashMap[KoreanPos, CharArraySet]

    map += Noun -> readWords(
      "noun/nouns.txt", "noun/entities.txt", "noun/spam.txt",
      "noun/names.txt", "noun/twitter.txt", "noun/lol.txt",
      "noun/slangs.txt", "noun/company_names.txt",
      "noun/foreign.txt", "noun/geolocations.txt", "noun/profane.txt",
      "substantives/given_names.txt", "noun/kpop.txt", "noun/bible.txt",
      "noun/wikipedia_title_nouns.txt", "noun/pokemon.txt"
    )
    map += Verb -> conjugatePredicates(readWordsAsSet("verb/verb.txt"))
    map += Adjective -> conjugatePredicates(readWordsAsSet("adjective/adjective.txt"), isAdjective = true)
    map += Adverb -> readWords("adverb/adverb.txt")
    map += Determiner -> readWords("aux/determiner.txt")
    map += Exclamation -> readWords("aux/exclamation.txt")
    map += Josa -> readWords("josa/josa.txt")
    map += Eomi -> readWords("verb/eomi.txt")
    map += PreEomi -> readWords("verb/pre_eomi.txt")
    map += Conjunction -> readWords("aux/conjunctions.txt")
    map += NounPrefix -> readWords("substantives/noun_prefix.txt")
    map += VerbPrefix -> readWords("verb/verb_prefix.txt")
    map += Suffix -> readWords("substantives/suffix.txt")
    map
  }

  lazy val nameDictionay = Map(
    'family_name -> readWords("substantives/family_names.txt"),
    'given_name -> readWords("substantives/given_names.txt")
  )

  lazy val typoDictionaryByLength = readWordMap("typos/typos.txt").groupBy {
    case (key: String, value: String) => key.length
  }
}

