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

data class AprsPath(val pathSegments: List<PathSegment>) {
    override fun toString(): String {
        return pathSegments.joinToString("")
    }

    companion object {
        /** Convenience method for getting the path required for transmission of packets directly
         *  from the client to APRS-IS. Should always return "TCPIP*" */
        fun directToAprsIs(): AprsPath {
            return AprsPath(listOf(PathSegment(Ax25Address("TCPIP", null), true)))
        }
    }
}

data class PathSegment(val address: Ax25Address, val digipeated: Boolean) {
    override fun toString(): String {
        return ",%s%s".format(address, if (digipeated) "*" else "")
    }
}
