package org.akinosoft.akinoclock.util.net

sealed class FetchResult {
    data class Success(val bytes: ByteArray, val lastModifiedMillis: Long?) : FetchResult()
    data object NotModified : FetchResult()
    data class Failure(val reason: Reason) : FetchResult() {
        sealed class Reason {
            data class Http(val code: Int) : Reason()
            data class Io(val message: String?) : Reason()
            data object TooLarge : Reason()
        }
    }
}
