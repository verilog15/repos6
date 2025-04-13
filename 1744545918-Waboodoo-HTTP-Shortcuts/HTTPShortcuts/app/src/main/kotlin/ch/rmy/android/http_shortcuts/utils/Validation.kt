package ch.rmy.android.http_shortcuts.utils

import android.net.Uri
import ch.rmy.android.http_shortcuts.variables.Variables.VARIABLE_ID_REGEX

object Validation {

    fun isAcceptableHttpUrl(url: String) =
        url.matches("^(http(s?)://.+)|((h(t(t(p(s)?)?)?)?)?\\{\\{$VARIABLE_ID_REGEX\\}\\}.*)".toRegex(RegexOption.IGNORE_CASE))

    fun isAcceptableUrl(url: String) =
        url.isNotEmpty() && url != "http://" && url != "https://"

    fun isValidUrl(uri: Uri) =
        uri.scheme?.isNotEmpty() == true &&
            uri.host?.contains("[{}<>\"']".toRegex()) != true

    fun isValidHttpUrl(uri: Uri) =
        isValidUrl(uri) &&
            !uri.host.isNullOrEmpty() &&
            (
                uri.scheme?.let { scheme ->
                    scheme.equals("http", ignoreCase = true) ||
                        scheme.equals("https", ignoreCase = true)
                } == true
                )

    fun isValidInHeaderName(c: Char): Boolean =
        c in '!'..'~'

    fun isValidInHeaderValue(c: Char): Boolean =
        (c > '\u001f' || c == '\t') && c < '\u007f'
}
