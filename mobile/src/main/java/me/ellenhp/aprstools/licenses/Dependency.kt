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

package me.ellenhp.aprstools.licenses

import org.yaml.snakeyaml.Yaml

data class Dependency(val artifact: String? = null,
                      val name: String? = null,
                      val copyrightHolder: String? = null,
                      val license: String? = null,
                      val licenseUrl: String? = null,
                      val url: String? = null) {

    constructor(attributes: LinkedHashMap<String, String>) : this(
            attributes.getOrDefault("artifact", null),
            attributes.getOrDefault("name", null),
            attributes.getOrDefault("copyrightHolder", null),
            attributes.getOrDefault("license", null),
            attributes.getOrDefault("licenseUrl", null),
            attributes.getOrDefault("url", null))


    companion object {
        fun parseFile(file: String): List<Dependency>? {
            val yaml = Yaml()
            val maps = yaml.load<List<LinkedHashMap<String, String>>>(file)
            return maps.toList().map { Dependency(it) }
        }
    }
}