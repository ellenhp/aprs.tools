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

package me.ellenhp.aprstools.tnc

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class KissTnc(private val inputStream: InputStream, private val outputStream: OutputStream) : Tnc {

    private val parser = KissFrameParser()

    @Throws(IOException::class)
    override fun readFrame(): KissFrame? {
        while (true) {
            val b = inputStream.read()
            if (b == -1) {
                // End of stream is reached without a complete frame.
                return null
            }
            return parser.nextByte(b)
        }
    }

    private class KissFrameParser {

        private var inFrame = false
        private val rawBytes = mutableListOf<Int>()
        private val frameBuffer = mutableListOf<Int>()

        /** Return the last byte seen by the parser or null if there is no data yet. */
        private val lastByte: Int?
            get() = if (rawBytes.size == 0) {
                null
            } else {
                rawBytes[rawBytes.size - 1]
            }

        /** Takes a new byte and returns a complete frame or null.  */
        internal fun nextByte(b: Int): KissFrame? {
            // Take an action based on the value of the byte and the current state of the parser.
            when (b) {
                FEND -> if (inFrame) {
                    if (inFESC()) {
                        // An escaped FEND inside a frame should be added to the frameBuffer.
                        frameBuffer.add(FEND)
                    } else if (lastByte != FEND) {
                        // This check ensures that back-to-back FENDs at the start of a frame are ignored per the spec.
                        return finishKissFrame()
                    }
                } else {
                    inFrame = true
                }
                FESC -> if (inFESC()) {
                    // An escaped FESC should be added to the frameBuffer.
                    frameBuffer.add(FESC)
                }
                else -> if (inFrame) {
                    frameBuffer.add(b)
                }
            }

            // Always add to rawBytes.
            rawBytes.add(b)
            return null
        }

        private fun inFESC(): Boolean {
            return lastByte == FESC
        }

        private fun resetState() {
            inFrame = false
            rawBytes.clear()
            frameBuffer.clear()
        }

        private fun finishKissFrame(): KissFrame {
            val frame = KissFrame(frameBuffer[0], frameBuffer.drop(1))
            resetState()
            return frame
        }
    }

    companion object {
        private const val FEND = 0xC0
        private const val FESC = 0xDB
    }
}
