package com.music.spine

import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.AsyncFunctionBinding
import com.dokar.quickjs.binding.FunctionBinding
import com.dokar.quickjs.binding.define
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal object QuickJsExecutor {

    private const val TAG = "SpineJS"
    private val maxConcurrent = 4
    private val activeInstances = AtomicInteger(0)

    private val syncHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    suspend fun executeModuleExport(
        jsCode: String,
        functionName: String,
        args: List<String>,
        fetchBase: String = "",
    ): String {
        if (activeInstances.get() >= maxConcurrent) {
            throw IllegalStateException("Max concurrent QuickJS instances ($maxConcurrent) reached")
        }

        activeInstances.incrementAndGet()
        try {
            return withContext(Dispatchers.Default) {
                val qjs = QuickJs.create(Dispatchers.Default)
                qjs.maxStackSize = 512 * 1024L
                try {
                    bindConsole(qjs)
                    bindAsyncFetch(qjs, fetchBase)
                    qjs.evaluate<String>(POLYFILLS)

                    val cleanCode = preprocessModuleCode(jsCode)
                    Log.d(TAG, "preprocessed code length: ${cleanCode.length}")

                    val iifeResult = qjs.evaluate<String>(
                        """
                        var __spine_iife_error = null;
                        var __spine_mod = (function() {
                            try {
                                var module = { exports: {} };
                                var exports = module.exports;
                                var self = {};
                                $cleanCode
                                if (module.exports && (module.exports.searchTracks || module.exports.getTrackStreamUrl)) {
                                    return module.exports;
                                }
                                return {};
                            } catch(e) {
                                __spine_iife_error = e && e.message ? e.message : String(e);
                                return {};
                            }
                        })();
                        'ok'
                        """.trimIndent()
                    )

                    val iifeError = qjs.evaluate<String>("__spine_iife_error || 'none'")
                    if (iifeError != "none") {
                        Log.e(TAG, "IIFE error: $iifeError")
                    }

                    val availableKeys = qjs.evaluate<String>("Object.keys(__spine_mod).join(', ')")
                    Log.d(TAG, "Module exports: [$availableKeys]")

                    val hasFn = qjs.evaluate<String>("typeof __spine_mod['$functionName']")
                    Log.d(TAG, "Has $functionName: $hasFn")

                    val argsStr = args.joinToString(",")
                    qjs.evaluate<String>(
                        """
                        var __spine_result = null;
                        var __spine_error = null;
                        var __fn = __spine_mod['$functionName'];
                        if (!__fn) {
                            __spine_error = '$functionName not found. Available: ' + Object.keys(__spine_mod).join(', ');
                        } else {
                            __fn($argsStr).then(
                                function(r) {
                                    __spine_result = typeof r === 'string' ? r : JSON.stringify(r);
                                },
                                function(e) {
                                    __spine_error = e && e.message ? e.message : String(e);
                                }
                            );
                        }
                        """.trimIndent()
                    )

                    val rawResult = qjs.evaluate<String>(
                        """__spine_result != null ? __spine_result : JSON.stringify({ error: __spine_error || 'unknown error' })"""
                    )
                    Log.d(TAG, "Raw result (${rawResult.length} chars): ${rawResult.take(500)}")
                    rawResult
                } finally {
                    qjs.close()
                }
            }
        } finally {
            activeInstances.decrementAndGet()
        }
    }

    private fun preprocessModuleCode(jsCode: String): String {
        val code = jsCode.trim()

        val exportPattern = Regex("""^export\s+const\s+\w+\s*=\s*`""")
        val exportMatch = exportPattern.find(code)
        if (exportMatch != null) {
            val contentStart = exportMatch.range.last + 1
            var i = contentStart
            while (i < code.length) {
                if (code[i] == '\\' && i + 1 < code.length) {
                    i += 2
                    continue
                }
                if (code[i] == '`') {
                    return code.substring(contentStart, i).trim()
                }
                i++
            }
        }

        var result = code
        result = result.replace(Regex("""\bexport\s+default\s+(?=function|class|const|let|var|async)"""), "")
        result = result.replace(Regex("""\bexport\s+(const|let|var|function|class|async)\b"""), "$1")
        result = result.replace(Regex("""\bexport\s*\{[^}]*\}\s*;?"""), "")
        return result
    }

    private fun bindConsole(qjs: QuickJs) {
        qjs.define("console") {
            function("log", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    Log.d(TAG, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("error", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    Log.e(TAG, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("warn", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    Log.w(TAG, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
            function("info", object : FunctionBinding<Unit> {
                override fun invoke(args: Array<Any?>) {
                    Log.i(TAG, args.joinToString(" ") { it?.toString() ?: "null" })
                }
            })
        }
    }

    private suspend fun bindAsyncFetch(qjs: QuickJs, fetchBase: String) {
        qjs.define("__spine") {
            asyncFunction("fetch", object : AsyncFunctionBinding<String> {
                override suspend fun invoke(args: Array<Any?>): String {
                    val url = resolveUrl(
                        args[0]?.toString() ?: throw IllegalArgumentException("fetch requires a URL"),
                        fetchBase
                    )
                    val method = args[1]?.toString() ?: "GET"
                    val headersJson = args[2]?.toString() ?: "{}"
                    val body = args[3]?.toString()
                    return fetchUrlSync(url, method, headersJson, body)
                }
            })
        }
        qjs.evaluate<Unit>(
            """
            var fetch = async function(url, options) {
                var method = 'GET';
                var headers = '{}';
                var body = null;
                if (options) {
                    method = options.method || 'GET';
                    if (options.headers) {
                        if (typeof options.headers === 'string') {
                            headers = options.headers;
                        } else {
                            try { headers = JSON.stringify(options.headers); } catch(e) { headers = '{}'; }
                        }
                    }
                    if (options.body !== undefined && options.body !== null) {
                        body = typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
                    }
                }
                return await __spine.fetch(url, method, headers, body);
            };
            """.trimIndent()
        )
    }

    private fun resolveUrl(url: String, base: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (base.isEmpty()) return url
        return if (url.startsWith("/")) {
            val scheme = base.substringBefore("://")
            val host = base.substringAfter("://").substringBefore("/")
            "$scheme://$host$url"
        } else {
            "$base/$url"
        }
    }

    private fun fetchUrlSync(url: String, method: String, headersJson: String, body: String?): String {
        return try {
            val builder = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                )

            try {
                val headersObj = JSONObject(headersJson)
                for (key in headersObj.keys()) {
                    val value = headersObj.optString(key, "")
                    if (key.equals("user-agent", ignoreCase = true)) continue
                    builder.header(key, value)
                }
            } catch (_: Exception) {}

            when (method.uppercase()) {
                "POST" -> {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    builder.post((body ?: "").toRequestBody(mediaType))
                }
                "PUT" -> {
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    builder.put((body ?: "").toRequestBody(mediaType))
                }
                "DELETE" -> builder.delete()
                "HEAD" -> builder.head()
                else -> builder.get()
            }

            syncHttpClient.newCall(builder.build()).execute().use { response ->
                response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetch failed: $method $url", e)
            throw e
        }
    }

    private const val POLYFILLS = """
        if (typeof AbortController === 'undefined') {
            var AbortController = function() { this.signal = { aborted: false }; };
            AbortController.prototype.abort = function() { this.signal.aborted = true; };
        }

        if (typeof Object.assign !== 'function') {
            Object.assign = function(target) {
                if (target == null) throw new TypeError('Cannot convert undefined or null to object');
                var to = Object(target);
                for (var i = 1; i < arguments.length; i++) {
                    var source = arguments[i];
                    if (source != null) {
                        for (var key in source) {
                            if (Object.prototype.hasOwnProperty.call(source, key)) {
                                to[key] = source[key];
                            }
                        }
                    }
                }
                return to;
            };
        }

        if (typeof Promise.any !== 'function') {
            Promise.any = function(promises) {
                return new Promise(function(resolve, reject) {
                    var errors = [];
                    var remaining = promises.length;
                    if (remaining === 0) { reject(new AggregateError([], 'All promises were rejected')); return; }
                    promises.forEach(function(p, i) {
                        Promise.resolve(p).then(resolve, function(e) {
                            errors[i] = e;
                            remaining--;
                            if (remaining === 0) reject(new AggregateError(errors, 'All promises were rejected'));
                        });
                    });
                });
            };
        }

        if (typeof Promise.allSettled !== 'function') {
            Promise.allSettled = function(promises) {
                return Promise.all(promises.map(function(p) {
                    return Promise.resolve(p).then(
                        function(value) { return { status: 'fulfilled', value: value }; },
                        function(reason) { return { status: 'rejected', reason: reason }; }
                    );
                }));
            };
        }

        if (typeof AggregateError === 'undefined') {
            var AggregateError = function(errors, message) {
                this.errors = errors;
                this.message = message || '';
                this.name = 'AggregateError';
            };
            AggregateError.prototype = Object.create(Error.prototype);
        }

        if (typeof atob === 'undefined') {
            var atob = function(input) {
                var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
                var str = String(input).replace(/=+$/, '');
                var output = '';
                for (var bc = 0, bs, buffer, idx = 0; buffer = str.charAt(idx++); ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer, bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0) {
                    buffer = chars.indexOf(buffer);
                }
                return output;
            };
        }

        if (typeof setTimeout === 'undefined') {
            var setTimeout = function(fn, ms) { fn(); return 0; };
        }
        if (typeof clearTimeout === 'undefined') {
            var clearTimeout = function(id) {};
        }

        if (typeof URL === 'undefined') {
            var URL = function(url, base) {
                this.href = url;
                try {
                    var a = url.replace(/^[^:]+:/, 'http:');
                    var match = a.match(/^\/\/([^/]+)(\/.*)?$/);
                    if (match) {
                        this.hostname = match[1];
                        this.pathname = match[2] || '/';
                    } else {
                        var m2 = a.match(/^https?:\/\/([^/]+)(\/.*)?$/);
                        if (m2) { this.hostname = m2[1]; this.pathname = m2[2] || '/'; }
                    }
                } catch(e) {}
            };
        }
    """
}
