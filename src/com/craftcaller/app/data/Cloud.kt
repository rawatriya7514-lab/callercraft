package com.craftcaller.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Community caller-ID directory in the cloud.
 *
 * Two interchangeable implementations:
 *  - [FireRepo] — live Firebase Firestore via its REST API driven by Retrofit
 *    (collections `users`, `reports`).
 *  - [DemoRepo] — fully working offline community used until a Firebase
 *    project is attached via assets/firebase_config.json.
 */
interface CloudRepo {
    val label: String
    val configured: Boolean
    suspend fun publishDirectory(entries: List<PublishEntry>): Boolean
    suspend fun lookup(number: String): Pair<String, String>?
    suspend fun reportSpam(number: String, reason: String): Int?
    suspend fun spamCounts(): Map<String, Int>
    suspend fun topSpammers(limit: Int = 20): List<SpamReportEntry>
}

data class PublishEntry(val number: String, val name: String, val city: String = "")

/** Iterate a Firestore list/runQuery response and map its documents. */
private inline fun <T> parseDocs(raw: String, transform: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>()
    runCatching {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.has("document")) out += transform(item.getJSONObject("document"))
        }
    }
    return out
}

object FireJson {
    fun fields(contact: Map<String, Any?>): RequestBody {
        val f = JSONObject()
        for ((k, v) in contact) {
            val cell = JSONObject()
            when (v) {
                is Int -> cell.put("integerValue", v.toString())
                is String -> cell.put("stringValue", v)
                is Boolean -> cell.put("booleanValue", v)
                else -> cell.put("stringValue", v.toString())
            }
            f.put(k, cell)
        }
        return JSONObject().put("fields", f).toString().asJson()
    }

    private fun String.asJson(): RequestBody = toRequestBody("application/json".toMediaType())

    fun docField(doc: JSONObject, key: String): String {
        val f = doc.optJSONObject("fields") ?: return ""
        val e = f.optJSONObject(key) ?: return ""
        return e.optString("stringValue", e.optString("integerValue", ""))
    }

    fun runQueryBody(collection: String, key: String, value: String): RequestBody = JSONObject()
        .put("structuredQuery", JSONObject()
            .put("from", JSONArray().put(JSONObject().put("collectionId", collection)))
            .put("where", JSONObject().put(
                "fieldFilter", JSONObject()
                    .put("field", JSONObject().put("fieldPath", key))
                    .put("op", "EQUAL")
                    .put("value", JSONObject().put("stringValue", value))
            ))
            .put("limit", 1)
        ).toString().let { it.toRequestBody("application/json".toMediaType()) }
}

object FireConfig {
    /**
     * Reads assets/firebase_config.json in google-services style
     * (api_key, project_id, optional database_url). Returns null when absent.
     */
    fun load(ctx: Context): FireCfg? {
        return try {
            val json = ctx.assets.open("firebase_config.json").bufferedReader().readText()
            val o = JSONObject(json)
            val key = o.optString("api_key")
            val project = o.optString("project_id")
            if (key.isEmpty() || project.isEmpty()) null
            else FireCfg(key, project, o.optString("database_url"))
        } catch (_: Exception) {
            null
        }
    }
}

data class FireCfg(val apiKey: String, val projectId: String, val databaseUrl: String)

// ---------------------------------------------------------------- Firestore via Retrofit

interface FireApi {
    @POST("v1/projects/{project}/databases/(default)/documents/users")
    suspend fun createUser(
        @Path("project") project: String,
        @Query("key") apiKey: String,
        @Body body: RequestBody
    ): ResponseBody

    @POST("v1/projects/{project}/databases/(default)/documents:runQuery")
    suspend fun runQuery(
        @Path("project") project: String,
        @Query("key") apiKey: String,
        @Body body: RequestBody
    ): ResponseBody

    @POST("v1/projects/{project}/databases/(default)/documents/reports/{doc}")
    suspend fun putReport(
        @Path("project") project: String,
        @Path("doc") doc: String,
        @Query("key") apiKey: String,
        @Body body: RequestBody
    ): ResponseBody

    @GET("v1/projects/{project}/databases/(default)/documents/reports")
    suspend fun listReports(
        @Path("project") project: String,
        @Query("pageSize") pageSize: Int = 300,
        @Query("key") apiKey: String
    ): ResponseBody
}

class FireRepo(private val cfg: FireCfg) : CloudRepo {
    override val label = "Firebase Firestore · live"
    override val configured = true

    private val api: FireApi = Retrofit.Builder()
        .baseUrl("https://firestore.googleapis.com/")
        .client(
            OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        )
        .build()
        .create(FireApi::class.java)

    override suspend fun lookup(number: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val body = FireJson.runQueryBody("users", "number", number)
        val raw = api.runQuery(cfg.projectId, cfg.apiKey, body)
        val arr = JSONArray(raw.string())
        if (arr.length() == 0) return@withContext null
        val doc = arr.getJSONObject(0)
        if (!doc.has("document")) return@withContext null
        val name = FireJson.docField(doc.getJSONObject("document"), "name")
        val city = FireJson.docField(doc.getJSONObject("document"), "city")
        if (name.isEmpty()) null else name to city
    }

    override suspend fun publishDirectory(entries: List<PublishEntry>): Boolean =
        withContext(Dispatchers.IO) {
            var ok = true
            for (e in entries) {
                try {
                    api.createUser(cfg.projectId, cfg.apiKey, FireJson.fields(mapOf(
                        "number" to e.number, "name" to e.name, "city" to e.city
                    )))
                    Db.putUser(e.number, e.name, e.city, "cloud")
                } catch (t: Throwable) {
                    ok = false
                }
            }
            ok
        }

    override suspend fun reportSpam(number: String, reason: String): Int? =
        withContext(Dispatchers.IO) {
            try {
                val docId = number.replace("+", "P")
                val raw = api.runQuery(
                    cfg.projectId, cfg.apiKey,
                    FireJson.runQueryBody("reports", "number", number)
                )
                var current = 0
                val arr = JSONArray(raw.string())
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    if (item.has("document")) {
                        current = FireJson.docField(item.getJSONObject("document"), "count").toIntOrNull() ?: 0
                        break
                    }
                }
                val total = current + 1
                api.putReport(
                    cfg.projectId, docId, cfg.apiKey,
                    FireJson.fields(mapOf(
                        "number" to number, "count" to total, "reason" to reason
                    ))
                )
                total
            } catch (_: Throwable) {
                null
            }
        }

    override suspend fun spamCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        val raw = try { api.listReports(cfg.projectId, apiKey = cfg.apiKey).string() } catch (_: Throwable) { return@withContext emptyMap() }
        parseDocs(raw) { d ->
            FireJson.docField(d, "number") to
                    (FireJson.docField(d, "count").toIntOrNull() ?: 0)
        }.toMap()
    }

    override suspend fun topSpammers(limit: Int): List<SpamReportEntry> = withContext(Dispatchers.IO) {
        val raw = try { api.listReports(cfg.projectId, apiKey = cfg.apiKey).string() } catch (_: Throwable) { return@withContext emptyList() }
        parseDocs(raw) { d ->
            SpamReportEntry(
                FireJson.docField(d, "number"),
                FireJson.docField(d, "count").toIntOrNull() ?: 0,
                FireJson.docField(d, "reason")
            )
        }.filter { it.reports >= SpamThreshold.COUNT }
            .sortedByDescending { it.reports }
            .take(limit)
    }
}

// ---------------------------------------------------------------- offline demo community

class DemoRepo : CloudRepo {
    override val label = "Community DB (offline demo)"
    override val configured = false

    override suspend fun publishDirectory(entries: List<PublishEntry>): Boolean {
        withContext(Dispatchers.IO) {
            entries.forEach { Db.putUser(it.number, it.name, it.city, "demo-cloud") }
        }
        return true
    }

    override suspend fun lookup(number: String): Pair<String, String>? =
        withContext(Dispatchers.IO) {
            Db.lookupUser(number).let {
                if (it != null && it.first.isNotEmpty()) it else null
            }
        }

    override suspend fun reportSpam(number: String, reason: String): Int =
        withContext(Dispatchers.IO) {
            Db.putReport(number, 1, reason)
            Db.report(number)
        }

    override suspend fun spamCounts(): Map<String, Int> = emptyMap()

    override suspend fun topSpammers(limit: Int): List<SpamReportEntry> =
        withContext(Dispatchers.IO) {
            Db.topReports(limit).sortedByDescending { it.reports }.take(limit)
        }
}

/** Backend selector: live Firestore while a Firebase config is attached, demo otherwise. */
object Cloud {
    @Volatile
    var repo: CloudRepo = DemoRepo()
        private set

    fun init(ctx: android.content.Context) {
        repo = FireConfig.load(ctx)?.let { FireRepo(it) } ?: DemoRepo()
    }
}
