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

package me.ellenhp.aprstools

import java.util.stream.Collector
import com.google.common.collect.ImmutableList

fun Any?.discard() = Unit

class Utils {
    companion object {
        fun <T> toImmutableList(): Collector<T, ImmutableList.Builder<T>, ImmutableList<T>>? {
            val supplier = {ImmutableList.builder<T>()}
            val accumulator = {builder:ImmutableList.Builder<T>, item: T -> builder.add(item).discard()}
            val combiner = {builder1: ImmutableList.Builder<T>, builder2: ImmutableList.Builder<T> -> builder1.addAll(builder2.build())}
            val finisher = {builder: ImmutableList.Builder<T> -> builder.build()}
            val collectorCharacteristics: Array<Collector.Characteristics>
            return Collector.of(supplier, accumulator, combiner, finisher, arrayOf(Collector.Characteristics.UNORDERED))
        }
    }
}