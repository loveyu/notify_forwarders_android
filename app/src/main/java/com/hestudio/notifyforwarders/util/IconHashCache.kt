package com.hestudio.notifyforwarders.util

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * 图标哈希缓存
 * 提供内存缓存和SQLite持久化缓存两层，用于存储 iconMd5 → iconUrl 的映射
 * 防止重复的远程检查/上传请求
 */
object IconHashCache {
    private const val TAG = "IconHashCache"
    private const val DB_NAME = "icon_hash_cache.db"
    private const val DB_VERSION = 1
    private const val TABLE_NAME = "icon_url_cache"
    private const val COLUMN_MD5 = "icon_md5"
    private const val COLUMN_URL = "icon_url"
    private const val COLUMN_TIMESTAMP = "created_at"

    private const val DEFAULT_MEMORY_CACHE_SIZE = 2000

    // LRU内存缓存
    private val memoryCache = object : LinkedHashMap<String, String>(16, 0.75f, true) {
        private var maxSize = DEFAULT_MEMORY_CACHE_SIZE

        fun setMaxSize(size: Int) {
            maxSize = size
        }

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > maxSize
        }
    }

    // 用于线程安全的内存缓存访问锁
    private val memoryCacheLock = Any()

    @Volatile
    private var memoryCacheEnabled = true

    @Volatile
    private var sqliteCacheEnabled = true

    @Volatile
    private var dbHelper: DatabaseHelper? = null

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                    $COLUMN_MD5 TEXT PRIMARY KEY,
                    $COLUMN_URL TEXT NOT NULL,
                    $COLUMN_TIMESTAMP INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_icon_md5 ON $TABLE_NAME ($COLUMN_MD5)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }

    /**
     * 初始化缓存
     * @param context 上下文
     * @param memoryEnabled 是否启用内存缓存
     * @param memoryMaxSize 内存缓存最大条目数
     * @param sqliteEnabled 是否启用SQLite缓存
     */
    fun init(context: Context, memoryEnabled: Boolean, memoryMaxSize: Int, sqliteEnabled: Boolean) {
        memoryCacheEnabled = memoryEnabled
        sqliteCacheEnabled = sqliteEnabled

        synchronized(memoryCacheLock) {
            memoryCache.setMaxSize(memoryMaxSize.coerceAtLeast(100))
            if (!memoryEnabled) {
                memoryCache.clear()
            }
        }

        if (sqliteEnabled) {
            dbHelper = DatabaseHelper(context.applicationContext)
        } else {
            dbHelper?.close()
            dbHelper = null
        }

        Log.d(TAG, "缓存初始化: memory=$memoryEnabled(${memoryMaxSize}), sqlite=$sqliteEnabled")
    }

    /**
     * 从缓存获取图标URL
     * 先查内存缓存，再查SQLite缓存
     * @param iconMd5 图标MD5哈希
     * @return 缓存的URL，不存在返回null
     */
    fun get(iconMd5: String): String? {
        // 1. 查内存缓存
        if (memoryCacheEnabled) {
            synchronized(memoryCacheLock) {
                memoryCache[iconMd5]?.let { url ->
                    Log.d(TAG, "内存缓存命中: $iconMd5")
                    return url
                }
            }
        }

        // 2. 查SQLite缓存
        if (sqliteCacheEnabled) {
            val db = dbHelper?.readableDatabase ?: return null
            try {
                db.query(
                    TABLE_NAME,
                    arrayOf(COLUMN_URL),
                    "$COLUMN_MD5 = ?",
                    arrayOf(iconMd5),
                    null, null, null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val url = cursor.getString(0)
                        // 回填内存缓存
                        if (memoryCacheEnabled && url != null) {
                            synchronized(memoryCacheLock) {
                                memoryCache[iconMd5] = url
                            }
                        }
                        Log.d(TAG, "SQLite缓存命中: $iconMd5")
                        return url
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "SQLite查询失败", e)
            }
        }

        return null
    }

    /**
     * 存入缓存
     * 同时写入内存和SQLite
     * @param iconMd5 图标MD5哈希
     * @param url 图标URL
     */
    fun put(iconMd5: String, url: String) {
        // 1. 写内存缓存
        if (memoryCacheEnabled) {
            synchronized(memoryCacheLock) {
                memoryCache[iconMd5] = url
            }
        }

        // 2. 写SQLite缓存
        if (sqliteCacheEnabled) {
            val db = dbHelper?.writableDatabase ?: return
            try {
                db.execSQL(
                    "INSERT OR REPLACE INTO $TABLE_NAME ($COLUMN_MD5, $COLUMN_URL, $COLUMN_TIMESTAMP) VALUES (?, ?, ?)",
                    arrayOf<Any>(iconMd5, url, System.currentTimeMillis())
                )
                Log.d(TAG, "SQLite缓存写入: $iconMd5")
            } catch (e: Exception) {
                Log.e(TAG, "SQLite写入失败", e)
            }
        }
    }

    /**
     * 检查是否存在缓存
     */
    fun contains(iconMd5: String): Boolean {
        return get(iconMd5) != null
    }

    /**
     * 清空所有缓存
     */
    fun clearAll() {
        synchronized(memoryCacheLock) {
            memoryCache.clear()
        }

        val db = dbHelper?.writableDatabase
        if (db != null) {
            try {
                db.delete(TABLE_NAME, null, null)
                Log.d(TAG, "SQLite缓存已清空")
            } catch (e: Exception) {
                Log.e(TAG, "清空SQLite缓存失败", e)
            }
        }
    }
}
