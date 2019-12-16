package com.mpc.scalats.core

import com.mpc.scalats.configuration.Config
import scala.reflect.runtime.universe._

/**
  * Created by Milosz on 11.06.2016.
  */
object TypeScriptGenerator {

  private def updateConfig(c: Config): Config = {
    if (c.emitIoTs) {
      c.copy(
        emitClasses = false,
        emitInterfaces = true,
        optionToNullable = false,
        optionToUndefined = false)
    } else {
      c
    }
  }

  def generateFromClassNames(
    classNames: List[String],
    logger: Logger,
    classLoader: ClassLoader = getClass.getClassLoader
  )(c: Config) = {
    val mirror = runtimeMirror(classLoader)
    val types = classNames.map { className =>
      println(s"className = $className")
      mirror.staticClass(className).toType
    }

    generate(types, logger, mirror)(updateConfig(c))
  }

  def generate(caseClasses: List[Type], logger: Logger, mirror: Mirror)(c: Config) = {
    implicit val config: Config = updateConfig(c)
    val outputStream = config.outputStream.getOrElse(Console.out)
    val scalaParser = new ScalaParser(logger, mirror)
    val scalaTypes = scalaParser.parseTypes(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaTypes)

    val emitter: Emitter = if (config.emitIoTs) {
      outputStream.println("""import * as t from "io-ts";""")
      outputStream.println("""import { optionFromNullable } from "io-ts-types/lib/optionFromNullable";""")
      outputStream.println("""import { DateFromISOString } from "io-ts-types/lib/DateFromISOString";""".stripMargin)
      outputStream.println()

      new IoTsEmitter(config)
    } else {
      new TypeScriptEmitter(config)
    }

    emitter.emit(typeScriptInterfaces, outputStream)
  }
}
