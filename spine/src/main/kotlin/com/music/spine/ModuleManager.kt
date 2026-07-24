package com.music.spine

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import timber.log.Timber

class ModuleManager {

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            expectSuccess = false
        }
    }

    private val loadedModules = mutableMapOf<String, LoadedModule>()

    data class LoadedModule(
        val module: SpineModule,
        val jsCode: String,
        val baseUrl: String,
    )

    suspend fun fetchIndex(sourceUrl: String): Result<List<SpineModule>> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Fetching index from $sourceUrl")
            val resp = client.get(sourceUrl)
            if (resp.status != HttpStatusCode.OK) {
                throw Exception("HTTP ${resp.status.value} from $sourceUrl")
            }
            val body = resp.bodyAsText()
            val index = json.decodeFromString<ModuleIndex>(body)
            val modules = index.allModules
            Timber.tag(TAG).d("Fetched ${modules.size} modules from $sourceUrl")
            modules
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to fetch index from $sourceUrl")
        }
    }

    suspend fun loadModule(module: SpineModule, resolveBaseUrl: suspend (String) -> String = { it }): Result<LoadedModule> = withContext(Dispatchers.IO) {
        val cached = loadedModules[module.id]
        if (cached != null) return@withContext Result.success(cached)

        runCatching {
            val downloadUrl = if (module.download.startsWith("http")) {
                module.download
            } else {
                val base = resolveBaseUrl(module.download)
                "$base/${module.download}"
            }

            Timber.tag(TAG).d("Downloading module ${module.id} from $downloadUrl")
            val resp = client.get(downloadUrl)
            if (resp.status != HttpStatusCode.OK) {
                throw Exception("HTTP ${resp.status.value} downloading module ${module.id}")
            }
            val jsCode = resp.bodyAsText()
            val baseUrl = downloadUrl.substringBeforeLast("/")

            val loaded = LoadedModule(module = module, jsCode = jsCode, baseUrl = baseUrl)
            loadedModules[module.id] = loaded
            Timber.tag(TAG).d("Loaded module ${module.id} (${jsCode.length} bytes)")
            loaded
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to load module ${module.id}")
        }
    }

    suspend fun searchTracks(
        loaded: LoadedModule,
        query: String,
        limit: Int = 50,
        settings: Map<String, String> = emptyMap(),
    ): Result<ModuleSearchResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val settingsJson = settings.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
            val contextArg = "{settings:{value:{$settingsJson}}}"

            val result = QuickJsExecutor.executeModuleExport(
                jsCode = loaded.jsCode,
                functionName = "searchTracks",
                args = listOf("\"$query\"", limit.toString(), contextArg),
                fetchBase = loaded.baseUrl,
            )

            try {
                json.decodeFromString<ModuleSearchResponse>(result)
            } catch (e: Exception) {
                Timber.tag(TAG).w("Module ${loaded.module.id} returned non-JSON search result: ${result.take(300)}")
                throw e
            }
        }.onFailure {
            Timber.tag(TAG).e(it, "Module ${loaded.module.id} search failed for '$query'")
        }
    }

    suspend fun getStreamUrl(
        loaded: LoadedModule,
        trackId: String,
        settings: Map<String, String> = emptyMap(),
    ): Result<ModuleStreamResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val settingsJson = settings.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }
            val contextArg = "{settings:{value:{$settingsJson}}}"

            val result = QuickJsExecutor.executeModuleExport(
                jsCode = loaded.jsCode,
                functionName = "getTrackStreamUrl",
                args = listOf("\"$trackId\"", "\"\"", contextArg),
                fetchBase = loaded.baseUrl,
            )

            try {
                json.decodeFromString<ModuleStreamResponse>(result)
            } catch (e: Exception) {
                Timber.tag(TAG).w("Module ${loaded.module.id} returned non-JSON stream result: ${result.take(300)}")
                throw e
            }
        }.onFailure {
            Timber.tag(TAG).e(it, "Module ${loaded.module.id} stream failed for track $trackId")
        }
    }

    fun unloadModule(moduleId: String) {
        loadedModules.remove(moduleId)
    }

    fun unloadAll() {
        loadedModules.clear()
    }

    companion object {
        private const val TAG = "ModuleManager"
    }
}
