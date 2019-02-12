/*
 * Copyright (c) 2019 Ellen Poe
 *
 * This file is part of APRSTools.
 *
 * APRSTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * APRSTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprslib.parser

import me.ellenhp.aprslib.packet.*
import norswap.autumn.Grammar
import norswap.autumn.parsers.*

class AprsParser {

    fun parse(rawPacket: String): AprsPacket? {
        val grammar = AprsGrammar()
        return if (grammar.parse(rawPacket))
            grammar.frame_end(0)[0] as AprsPacket
        else
            null
    }
}

private class AprsGrammar : Grammar()
{
    override fun root() = packet()

    private fun packet() = build(
            syntax = { address() && ">".str && address() && path() && ":".str && informationField() },
            effect = {
                AprsPacket(source = it(0), dest = it(1), path = it(2), informationField = it(3))
            }
    )

    private fun informationField() = build(
            syntax = {
                seq {
                    build_str { char_any() } && // Data type
                            aprsData() &&
                            aprsDataExtension() &&
                            comment()
                }
            },
            effect = {
                val dataType: String = it(0)
                val aprsData: AprsData = it(1)
                val aprsDataExtension: AprsDataExtension? = it(2)
                val comment: String = it(3)
                AprsInformationField(dataType[0], aprsData, aprsDataExtension, comment)
            }
    )

    private fun comment() = build_str {
        repeat0 { char_any() }
    }

    private fun aprsData() = build(
            syntax = { repeat0 { choice { posit() || timestamp() } } },
            effect = {
                val aprsDataPoints : Array<Any?> = it
                fun deref(idx: Int) : Any? { return aprsDataPoints(idx) }
                if (aprsDataPoints.isNotEmpty())
                    AprsData(IntRange(0, aprsDataPoints.size - 1).map { i -> deref(i) as AprsDatum } )
                else
                    AprsData(emptyList())
            }
    )

    private fun posit() = build(
            syntax = {
                    // Latitude
                    latitude() &&
                    // Symbol table
                    build_str { char_any() } &&
                    // Latitude
                    longitude() &&
                    // Symbol id
                    build_str { char_any() }
            },
            effect = {
                val lat: AprsAngle = it(0)
                val symbolTable: String = it(1)
                val long: AprsAngle = it(2)
                val symbolId: String = it(3)
                AprsPosition(
                        position = AprsLatLng(lat.angle, long.angle, lat.ambiguity),
                        symbol = AprsSymbol(symbolTable[0], symbolId[0])
                )
            }
    )

    private fun latitude() = build(
            syntax = {
                seq {
                    build_str { repeat(2) { digitOrSpace() } } &&
                    build_str { repeat(2) { digitOrSpace() } } &&
                    ".".str &&
                    build_str { repeat(2) { digitOrSpace() } } &&
                    build_str {
                        char_set("NS")
                    }
                }
            },
            effect = {
                val cardinalDir: String = it(3)
                AprsAngle(it(0), it(1), it(2), if (cardinalDir == "N") 1 else -1)
            }
    )


    private fun longitude() = build(
            syntax = {
                seq {
                    build_str { repeat(3) { digitOrSpace() } } &&
                    build_str { repeat(2) { digitOrSpace() } } &&
                    ".".str &&
                    build_str { repeat(2) { digitOrSpace() } } &&
                    build_str {
                        char_set("EW")
                    }
                }
            },
            effect = {
                val cardinalDir: String = it(3)
                AprsAngle(it(0), it(1), it(2), if (cardinalDir == "E") 1 else -1)
            }
    )

    private fun timestamp() = choice {
        timestampDhm() || timestampHms() || timestampMdhm()
    }

    private fun timestampDhm() = build(
            syntax = {
                build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        build_str { char_set("/z") }
            },
            effect = {
                val day: String = it(0)
                val hour: String = it(1)
                val minute: String = it(2)
                val timeZone: String = it(3)
                AprsTimestampDhm(
                        day.toInt(),
                        hour.toInt(),
                        minute.toInt(),
                        if (timeZone[0] == 'z') AprsTimezone.ZULU else AprsTimezone.LOCAL
                )
            }
    )

    private fun timestampHms() = build(
            syntax = {
                build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        "h".str
            },
            effect = {
                val hours: String = it(0)
                val minutes: String = it(1)
                val seconds: String = it(2)
                AprsTimestampHms(
                        hours.toInt(),
                        minutes.toInt(),
                        seconds.toInt()
                )
            }
    )

    private fun timestampMdhm() = build(
            syntax = {
                build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } } &&
                        build_str { repeat(2) { digit() } }
            },
            effect = {
                val month: String = it(0)
                val day: String = it(1)
                val hour: String = it(2)
                val minute: String = it(3)
                AprsTimestampMdhm(
                        month.toInt(),
                        day.toInt(),
                        hour.toInt(),
                        minute.toInt()
                )
            }
    )

    private fun aprsDataExtension() = build(
            syntax = { maybe { choice { powerHeightGain() || courseSpeed() } } },
            effect = { it(0) }
    )

    private fun courseSpeed() = build(
            syntax = {
                seq {
                    build_str { repeat(3) { digit() } } && // Course
                            "/".str && // Separator
                            build_str { repeat(3) { digit() } } // Speed
                }
            },
            effect = {
                val course: String = it(0)
                val speed: String = it(1)
                CourseSpeed(course.toInt(), speed.toInt())
            }
    )

    private fun powerHeightGain() = build(
            syntax = {
                seq {
                    "PHG".str &&
                            build_str { digit() } && // Power
                            build_str { digit() } && // Height
                            build_str { digit() } && // Gain
                            build_str { digit() } // Directivity
                }
            },
            effect = {
                val powerCode: String = it(0)
                val heightCode: String = it(1)
                val gainCode: String = it(2)
                val directivityCode: String = it(3)
                AprsPowerHeightGain(powerCode[0], heightCode[0], gainCode[0], directivityCode[0])
            }
    )

    private fun path() = build(
            syntax = { opt { repeat1 { pathSegment() } } },
            effect = {
                val pathSegments : Array<Any?> = it
                fun deref(idx: Int) : Any? { return pathSegments(idx) }
                AprsPath(IntRange(0, pathSegments.size - 1).map { i -> deref(i) as PathSegment })
            }
    )

    private fun pathSegment() = build(
            syntax = { seq { ",".str && address() && as_bool { "*".str } } },
            effect = { PathSegment(it(0), it(1)) }
    )

    private fun address() = build (
            syntax = { seq { addressWithoutSsid() && maybe { ssidSuffix() } } },
            effect = { Ax25Address(it(0), it(1)) }
    )

    private fun addressWithoutSsid() = build_str { repeat1 { addressChar() } }

    private fun ssidSuffix() = seq { "-".str && ssid() }

    private fun ssid() = build_str { repeat1 { addressChar() } }

    // While the AX.25 spec says uppercase only, this rule is reused for the q codes in the APRS path.
    private fun addressChar() = choice {
        char_range('a', 'z') ||
                char_range('A', 'Z') ||
                char_range('0', '9')
    }

    private fun digitOrSpace() = choice { char_set(" ") || digit() }
}

private data class AprsAngle(val degrees: String, val wholeMinutes: String, val hundredthsMinutes: String, val sign: Int) {
    val angle: Double = sign * (
            degrees.toDouble() +
            wholeMinutes.replace(' ', '5').toDouble() / 60 +
            hundredthsMinutes.replace(' ', '5').toDouble() / 6000)
    val ambiguity: AprsPositAmbiguity

    init {
        val numSpaces = (degrees + wholeMinutes + hundredthsMinutes).filter {it == ' '} .count()
        ambiguity = AprsPositAmbiguity.fromOmmittedSpaces(numSpaces)!!
    }

}