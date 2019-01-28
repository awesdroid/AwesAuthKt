package io.awesdroid.awesauthkt.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.json.JSONObject

/**
 * @auther Awesdroid
 */
val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 20) tag else tag.substring(0, 20)
    }

object Utils {
    fun prettyJson(jsonStr: String): String {
        val jsonElement = JsonParser().parse(jsonStr)
        return GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
    }

    fun prettyJson(json: JSONObject): String {
        val jso = JsonParser().parse(json.toString()).asJsonObject
        return GsonBuilder().setPrettyPrinting().create().toJson(jso)
    }
}