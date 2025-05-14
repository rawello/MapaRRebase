package com.rawello.mapar.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.rawello.mapar.data.database.AppDatabase
import com.rawello.mapar.data.models.Token
import com.rawello.mapar.data.network.ApiFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TokenManager private constructor(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "app_database"
    ).build()

    private val tokenDao = database.tokenDao()

    companion object {
        @Volatile
        private var instance: TokenManager? = null
        private const val MAX_RETRY_ATTEMPTS = 3

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context).also { instance = it }
            }
        }
    }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            try {
                initializeTokens()
            } catch (e: Exception) {
                Log.e("TokenManager", "Error initializing tokens: ${e.message}")
                ensureAnonymousToken()
            }
        }
    }

    private suspend fun initializeTokens() {
        val tokens = tokenDao.getAllTokens()
        
        when {
            tokens.isEmpty() -> {
                ensureAnonymousToken()
            }
            else -> {
                val userToken = tokens.find { it.name != "Anonymous" }
                val token = userToken?.token ?: tokenDao.getTokenByName("Anonymous")?.token

                if (token != null) {
                    if (!validateAndUpdateToken(token, userToken?.name)) {
                        // Если токен невалиден и это был пользовательский токен,
                        // удаляем его и получаем анонимный
                        logout()
                    }
                } else {
                    ensureAnonymousToken()
                }
            }
        }
    }

    private suspend fun validateAndUpdateToken(token: String, username: String?): Boolean {
        ApiFactory.TOKEN = token
        return try {
            if (ApiFactory.apiService.validateToken()) {
                true
            } else {
                if (username != "Anonymous") {
                    // Для пользовательского токена просто возвращаем false
                    false
                } else {
                    // Для анонимного токена пробуем получить новый
                    ensureAnonymousToken()
                    true
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun ensureAnonymousToken(retryAttempt: Int = 0) {
        try {
            val anonymousToken = ApiFactory.apiService.getAnonymousToken()
            tokenDao.deleteTokenByName("Anonymous") // Удаляем старый анонимный токен, если он есть
            tokenDao.insertToken(Token("Anonymous", anonymousToken.token))
            ApiFactory.TOKEN = anonymousToken.token
        } catch (e: Exception) {
            if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                // Повторяем попытку получения анонимного токена
                ensureAnonymousToken(retryAttempt + 1)
            } else {
                throw e
            }
        }
    }

    suspend fun login(username: String, token: String) {
        withContext(Dispatchers.IO) {
            if (validateAndUpdateToken(token, username)) {
                // Сохраняем токен пользователя только если он валиден
                tokenDao.deleteTokenByName("Anonymous") // Удаляем анонимный токен
                tokenDao.insertToken(Token(username, token))
                ApiFactory.TOKEN = token
            } else {
                throw IllegalStateException("Invalid user token")
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenDao.getAllTokens().forEach { token ->
                tokenDao.deleteToken(token)
            }
            ensureAnonymousToken()
        }
    }

    suspend fun isAnonymous(): Boolean {
        return withContext(Dispatchers.IO) {
            val userToken = tokenDao.getAllTokens().find { it.name != "Anonymous" }
            userToken == null
        }
    }

    suspend fun getCurrentToken(): String? {
        return withContext(Dispatchers.IO) {
            val userToken = tokenDao.getAllTokens().find { it.name != "Anonymous" }
            val token = userToken?.token ?: tokenDao.getTokenByName("Anonymous")?.token
            
            if (token != null && !validateAndUpdateToken(token, userToken?.name)) {
                // Если токен невалиден, возвращаем null
                null
            } else {
                token
            }
        }
    }

    suspend fun getCurrentUsername(): String? {
        return withContext(Dispatchers.IO) {
            val userToken = tokenDao.getAllTokens().find { it.name != "Anonymous" }
            userToken?.name.takeIf { it != "Anonymous" }
        }
    }
}
