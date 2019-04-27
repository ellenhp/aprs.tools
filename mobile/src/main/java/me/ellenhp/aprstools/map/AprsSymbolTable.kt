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

package me.ellenhp.aprstools.map

import android.content.Context
import android.graphics.*
import me.ellenhp.aprstools.R
import javax.inject.Inject

data class SymbolTableKey(val table: Char, val index: Char)

class AprsSymbolTable @Inject constructor(val context: Context) {

    val bitmapCache = HashMap<SymbolTableKey, Bitmap>()
    val symbolTableImg = BitmapFactory.decodeResource(context.resources, R.drawable.aprs_symbols)
    val symbolsPerRow = 16
    val symbolsPerColumn = 6
    val symbolSize = symbolTableImg.width / symbolsPerRow

    fun getSymbol(table: Char, index: Char): Bitmap {
        val key = SymbolTableKey(table, index)
        val existingBitmap = bitmapCache[key]
        existingBitmap?.let { return existingBitmap }

        val symbol = createSymbol(getSymbolPage(key), getSymbolIndex(key))

        if (symbolIsOverlayed(key)) {
            val canvas = Canvas(symbol)
            canvas.drawBitmap(
                    createSymbol(2, getOverlaySymbolIndex(key)),
                    Rect(0, 0, symbol.width, symbol.height),
                    Rect(0, 0, symbol.width, symbol.height),
                    Paint())
        }
        bitmapCache[key] = symbol
        return symbol
    }

    fun createSymbol(page: Int, indexInPage: Int): Bitmap {
        val pageOffset = page * symbolsPerColumn
        return Bitmap.createScaledBitmap(
                Bitmap.createBitmap(
                        symbolTableImg,
                        (indexInPage % symbolsPerRow) * symbolSize,
                        (pageOffset + indexInPage / symbolsPerRow) * symbolSize,
                        symbolSize,
                        symbolSize),
                symbolSize/2,
                symbolSize/2,
                true)
    }

    companion object {
        private fun getSymbolPage(key: SymbolTableKey): Int {
            return if (key.table == '/') 0 else 1
        }

        private fun getSymbolIndex(key: SymbolTableKey): Int {
            return key.index.toInt() - 33
        }

        private fun getOverlaySymbolIndex(key: SymbolTableKey): Int {
            return key.table.toInt() - 33
        }

        private fun symbolIsOverlayed(key: SymbolTableKey): Boolean {
            return key.table != '/' && key.table != '\\'
        }
    }

}