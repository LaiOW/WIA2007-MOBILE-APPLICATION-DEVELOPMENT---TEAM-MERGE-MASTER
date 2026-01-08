package com.example.myapplication

import android.net.Uri
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import io.github.jan.supabase.storage.upload
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.io.InputStream
import io.github.jan.supabase.gotrue.providers.builtin.Email // Import the Email provider
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.function.Function
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SupabaseManager {
    private const val SUPABASE_URL = "https://syuyeszaltkqewyeqwho.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_K47yeFMddp2Mv7-vA7rVyQ_CTuN6J7t"
    private const val POST_BUCKET_NAME = "Posts"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)

        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        })
    }

    interface AuthCallback {
        fun onComplete(success: Boolean, message: String?)
    }

    @JvmSuppressWildcards
    interface DatabaseCallback<T> {
        fun onSuccess(data: List<T>?)
        fun onError(message: String?)
    }

    interface StorageCallback {
        fun onSuccess(imageUrl: String)
        fun onError(message: String?)
    }

    fun runOnIo(task: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            task.invoke()
        }
    }

    fun <T> runOnIo(callable: Callable<T>, callback: (T) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = callable.call()
            withContext(Dispatchers.Main) {
                callback.invoke(result)
            }
        }
    }

    fun signUp(email: String, password: String, callback: AuthCallback) {
        runOnIo {
            try {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                withContext(Dispatchers.Main) {
                    callback.onComplete(true, "Account created")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onComplete(false, e.message)
                }
            }
        }
    }

    fun signIn(email: String, password: String, callback: AuthCallback) {
        runOnIo {
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                withContext(Dispatchers.Main) {
                    callback.onComplete(true, "Login Successful")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onComplete(false, e.message)
                }
            }
        }
    }

    fun signOut(callback: AuthCallback? = null) {
        runOnIo {
            try {
                client.auth.signOut()
                withContext(Dispatchers.Main) {
                    callback?.onComplete(true, "Signed out")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onComplete(false, e.message)
                }
            }
        }
    }

    fun getCurrentUserEmail(): String? {
        return client.auth.currentSessionOrNull()?.user?.email
    }

    fun isLoggedIn(): Boolean {
        // More robust check for session status
        val status = client.auth.sessionStatus.value
        return status is SessionStatus.Authenticated || client.auth.currentSessionOrNull() != null        
    }

    // Database Operations
    fun getPosts(callback: DatabaseCallback<Post>) {
        runOnIo {
            try {
                val response = client.postgrest["posts"].select().decodeList<Post>()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message)
                }
            }
        }
    }

    fun savePost(post: Post, callback: DatabaseCallback<Post>) {
        runOnIo {
            try {
                client.postgrest["posts"].insert(post)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message)
                }
            }
        }
    }

    // Storage Operations
    fun uploadImage(inputStream: InputStream, fileName: String, callback: StorageCallback) {
        runOnIo {
            try {
                val byteArray = inputStream.readBytes()
                val fullPath = "private/$fileName"
                client.storage.from(POST_BUCKET_NAME).upload(fullPath, byteArray, upsert = true)
                val publicUrl = client.storage.from(POST_BUCKET_NAME).publicUrl(fullPath)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(publicUrl)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message)
                }
            }
        }
    }

    
    // SOS Operations
    private fun getUsername(): String {
        return getCurrentUserEmail()?.split("@")?.firstOrNull() ?: "anonymous"
    }

    private fun getTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    fun recordSOSCall(xCoordinate: Double, yCoordinate: Double, callback: AuthCallback) {
        runOnIo {
            try {
                val username = getUsername()
                val time = getTime()
                val sosCall = SOSCall(
                    time = time,
                    username = username,
                    x_coordinate = xCoordinate,
                    y_coordinate = yCoordinate
                )
                client.postgrest["SOS calls"].insert(sosCall)
                withContext(Dispatchers.Main) {
                    callback.onComplete(true, "SOS call recorded successfully")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onComplete(false, "Failed to record SOS: ${e.message}")
                }
            }
        }
    }

    // Statistics Operations
    interface StatsCallback {
        fun onSuccess(casesCount: Int, userCount: Int)
        fun onError(message: String?)
    }

    fun getStatistics(callback: StatsCallback) {
        runOnIo {
            try {
                // Get total SOS cases count
                val sosCallsResponse = client.postgrest["SOS calls"].select().decodeList<SOSCall>()
                val casesCount = sosCallsResponse.size

                // Get unique user count from SOS calls
                val uniqueUsers = sosCallsResponse.map { it.username }.toSet()
                val userCount = uniqueUsers.size

                withContext(Dispatchers.Main) {
                    callback.onSuccess(casesCount, userCount)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message)
                }
            }
        }
    }

    interface SOSCallsCallback {
        fun onSuccess(sosCalls: List<SOSCall>)
        fun onError(message: String?)
    }

    fun getAllSOSCalls(callback: SOSCallsCallback) {
        runOnIo {
            try {
                val sosCallsResponse = client.postgrest["SOS calls"].select().decodeList<SOSCall>()
                withContext(Dispatchers.Main) {
                    callback.onSuccess(sosCallsResponse)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message)
                }
            }
        }
    }
}
