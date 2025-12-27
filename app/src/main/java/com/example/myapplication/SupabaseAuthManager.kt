package com.example.myapplication

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SupabaseAuthManager {
    private const val SUPABASE_URL = "https://syuyeszaltkqewyeqwho.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_K47yeFMddp2Mv7-vA7rVyQ_CTuN6J7t"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Auth)
    }

    interface AuthCallback {
        fun onComplete(success: Boolean, message: String?)
    }

    fun signUp(email: String, password: String, callback: AuthCallback) {
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
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
        CoroutineScope(Dispatchers.IO).launch {
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
    
    // Check if user is logged in (session exists and is valid)
    fun isLoggedIn(): Boolean {
        return client.auth.currentSessionOrNull() != null
    }
}
