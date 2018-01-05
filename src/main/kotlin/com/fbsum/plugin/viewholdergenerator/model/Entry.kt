package com.fbsum.plugin.viewholdergenerator.model

import java.util.regex.Pattern

class Entry(name: String, id: String) {

    companion object {
        private val sIdPattern = Pattern.compile("@\\+?(android:)?id/([^$]+)$", Pattern.CASE_INSENSITIVE)
        private val sValidityPattern = Pattern.compile("^([a-zA-Z_\\$][\\w\\$]*)$", Pattern.CASE_INSENSITIVE)
    }

    var id: String = ""
    var fullId: String = ""
    var isAndroidNS = false
    var fullName: String? = null // element name with package
    var name: String // element name
    var variableName: String = ""// name of variable

    var isValid = false
    var used = true

    init {
        // id
        val matcher = sIdPattern.matcher(id)
        if (matcher.find() && matcher.groupCount() > 0) {
            this.id = matcher.group(2)

            val androidNS = matcher.group(1)
            this.isAndroidNS = !(androidNS == null || androidNS.isEmpty())
        }

        // full id
        fullId = if (isAndroidNS) {
            "android.R.id.${this.id}"
        } else {
            "R.id.${this.id}"
        }

        // name
        val packages = name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (packages.size > 1) {
            this.fullName = name
            this.name = packages[packages.size - 1]
        } else {
            this.fullName = null
            this.name = name
        }

        this.variableName = generateVariableName()
    }

    /**
     * Generate field name if it's not done yet
     */
    private fun generateVariableName(): String {
        val words = this.id.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (i in words.indices) {
            val idTokens = words[i].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val chars = idTokens[idTokens.size - 1].toCharArray()
            if (i > 0) {
                chars[0] = Character.toUpperCase(chars[0])
            }
            sb.append(chars)
        }
        return sb.toString()
    }

    /**
     * Check validity of field name
     */
    fun checkValidity(): Boolean {
        val matcher = sValidityPattern.matcher(variableName)
        isValid = matcher.find()
        return isValid
    }

    override fun toString(): String {
        return "Entry(id='$id', fullId='$fullId', isAndroidNS=$isAndroidNS, fullName=$fullName, name='$name', variableName='$variableName', isValid=$isValid, used=$used)"
    }


}
