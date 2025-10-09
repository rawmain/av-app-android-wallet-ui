/*
 * Copyright (C) 2020 Newlogic Pte. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package eu.europa.ec.passportscanner.utils.extension


fun List<String>.arrayToString(): String {
    var temp = ""
    val iterator = this.iterator()
    while (iterator.hasNext()) {
        temp += iterator.next() + "\n"
    }
    if (temp.endsWith("\n")) {
        temp = temp.substring(0, temp.length - "\n".length)
    }
    return temp
}
