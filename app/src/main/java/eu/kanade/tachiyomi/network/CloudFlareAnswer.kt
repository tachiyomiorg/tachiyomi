package eu.kanade.tachiyomi.network

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CloudFlareAnswer {
    private fun _cf(script: String): Long {
        var result = ""
        val ss = script.split("\\(|\\)".toRegex())
        for (s in ss) {
            var _s: String
            if(s.isBlank() || s == "+") {
                continue
            } else {
                _s = s.replace("!+[]", "1")
                _s = _s.replace("!![]", "1")
                _s = _s.replace("![]", "0")
                _s = _s.replace("[]", "0")
                _s = _s.replace("+!![]", "10")
            }

            val s_ = _s.split("+").filter { it.isNotBlank() }
            when {
                s_.size > 1 -> result += s_.sumBy { it.toInt() }.toString()
                s_.size == 1 -> result += s_[0]
            }
        }
        return result.toLong()
    }

    private fun cf(script: String): Double {
        val ss = script.split("/")
        var result: Double = _cf(ss[0]).toDouble()
        for (i in ss.drop(1)) {
            val value = _cf(i)
            if(value > 0) {
                result /= value
            }
        }
        return result
    }

    fun cfa(html: String): Double? {
        val re1 = Pattern.compile("Checking your browser before accessing<\\/span>[\\s]+(.*?)[\\.\\s]+<\\/h1>").matcher(html)
        var host = ""
        while(re1.find()) host = re1.group(1)

        val re2 = Pattern.compile(".*?=\\{['|\"].*?['|\"]\\:([+\\/\\(\\)!\\[\\]]+)\\};").matcher(html)
        var anss = ""
        while(re2.find()) anss = re2.group(1)

        if(host.isBlank() || anss.isBlank()) return null

        var ans: Double = cf(anss)

        val re3 = Pattern.compile(".+?\\..+?([*+-]{1})=([+\\/\\(\\)!\\[\\]]+);").matcher(html)
        while (re3.find()) {
            when {
                re3.group(1) == "*" -> ans *= cf(re3.group(2))
                re3.group(1) == "+" -> ans += cf(re3.group(2))
                re3.group(1) == "-" -> ans -= cf(re3.group(2))
            }
        }

        val final = String.format("%.10f", ans + host.length).toDouble()

        TimeUnit.SECONDS.sleep(4)
        return final

    }
}
