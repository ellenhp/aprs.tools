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
 * along with APRSTools.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.ellenhp.aprslib.packet

interface AprsTimestamp : AprsDatum

enum class AprsTimezone(val code: Char) {
    ZULU('z'),
    LOCAL('/')
}

data class AprsTimestampDhm(val day: Int, val hour: Int, val minute: Int, val timeZone: AprsTimezone) : AprsTimestamp {
    override fun toString(): String {
        return "%02d%02d%02d%c".format(day, hour, minute, timeZone.code)
    }
}

data class AprsTimestampHms(val hour: Int, val minute: Int, val second: Int) : AprsTimestamp {
    override fun toString(): String {
        return "%02d%02d%02dh".format(hour, minute, second)
    }
}

data class AprsTimestampMdhm(val month: Int, val day: Int, val hour: Int, val minute: Int) : AprsTimestamp {
    override fun toString(): String {
        return "%02d%02d%02d%02d".format(month, day, hour, minute)
    }
}
