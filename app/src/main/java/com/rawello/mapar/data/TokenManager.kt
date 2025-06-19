package com.rawello.mapar.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.rawello.mapar.data.database.AppDatabase
import com.rawello.mapar.data.models.Token
import com.rawello.mapar.data.network.ApiFactory
import com.rawello.mapar.data.network.AuthInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class TokenManager private constructor(context: Context) {
    private val database = AppDatabase.getDatabase(context.applicationContext)

    private val tokenDao = database.tokenDao()
    private val userDao = database.userDao()

    companion object {
        @Volatile
        private var instance: TokenManager? = null
        private const val MAX_RETRY_ATTEMPTS = 3
        private var lastToken: String = ""

        fun getInstance(context: Context): TokenManager {
            return instance ?: synchronized(this) {
                instance ?: TokenManager(context).also { instance = it }
            }
        }

        fun buildOkHttpClient(tokenProvider: () -> String): OkHttpClient {
            val logging = HttpLoggingInterceptor { message ->
                Log.d("OkHttp", message)
            }
            logging.level = HttpLoggingInterceptor.Level.HEADERS
            return OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenProvider))
                .addInterceptor(logging)
                .build()
        }
    }

    suspend fun bindApiFactoryTokenProvider() {
        ApiFactory.updateTokenProvider {
            lastToken
        }
    }

    private suspend fun updateTokenEverywhere(token: String) {
        lastToken = token
        ApiFactory.updateTokenProvider { lastToken }
    }

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            bindApiFactoryTokenProvider()
            try {
                initializeTokens()
                val userToken = tokenDao.getAllTokens().find { it.name != "Anonymous" }?.token
                val token = userToken ?: tokenDao.getTokenByName("Anonymous")?.token
                if (token != null) updateTokenEverywhere(token)
            } catch (e: Exception) {
                Log.e("TokenManager", "Ошибка при инициализации токенов: ${e.message}")
                ensureAnonymousToken()
                val anonToken = tokenDao.getTokenByName("Anonymous")?.token
                if (anonToken != null) updateTokenEverywhere(anonToken)
            }
        }
    }

    private suspend fun initializeTokens() {
        Log.d("TokenManager", "[initializeTokens] Проверка токенов и пользователей в БД...")
        val tokens = tokenDao.getAllTokens()
        val users = userDao.getAllUsers()
        Log.d("TokenManager", "[initializeTokens] Токенов в БД: ${tokens.size}, пользователей: ${users.size}")
        when {
            tokens.isEmpty() && users.isEmpty() -> {
                Log.d("TokenManager", "[initializeTokens] Нет ни токена, ни пользователя — создаём анонимный токен")
                ensureAnonymousToken()
            }
            tokens.isEmpty() && users.isNotEmpty() -> {
                Log.d("TokenManager", "[initializeTokens] Есть пользователь, но нет токена — удаляем пользователя, создаём анонимный токен")
                userDao.deleteAllUsers()
                ensureAnonymousToken()
            }
            tokens.isNotEmpty() && users.isEmpty() -> {
                Log.d("TokenManager", "[initializeTokens] Есть токен, но нет пользователя — используем токен (анонимный)")
                val token = tokens.firstOrNull { it.name == "Anonymous" }?.token
                if (token != null) {
                    updateTokenEverywhere(token)
                } else {
                    Log.d("TokenManager", "[initializeTokens] Нет анонимного токена, пересоздаём")
                    ensureAnonymousToken()
                }
            }
            tokens.isNotEmpty() && users.isNotEmpty() -> {
                Log.d("TokenManager", "[initializeTokens] Есть и пользователь, и токен — используем их")
                val userToken = tokens.find { it.name != "Anonymous" }
                val token = userToken?.token ?: tokens.firstOrNull { it.name == "Anonymous" }?.token
                if (token != null) {
                    updateTokenEverywhere(token)
                    if (!validateAndUpdateToken(token, userToken?.name)) {
                        Log.d("TokenManager", "[initializeTokens] Токен невалиден, выполняем logout()")
                        logout()
                    }
                } else {
                    Log.d("TokenManager", "[initializeTokens] Нет токена, пересоздаём анонимный")
                    ensureAnonymousToken()
                }
            }
        }
    }

    private suspend fun validateAndUpdateToken(token: String, username: String?): Boolean {
        Log.d("TokenManager", "validateAndUpdateToken: token=$token, username=$username")
        updateTokenEverywhere(token)
        return true
    }

    private suspend fun ensureAnonymousToken(retryAttempt: Int = 0) {
        try {
            Log.d("TokenManager", "[ensureAnonymousToken] Попытка получить анонимный токен, попытка №$retryAttempt")
            val anonymousToken = ApiFactory.apiService.getAnonymousToken()
            Log.d("TokenManager", "[ensureAnonymousToken] Получен анонимный токен: ${anonymousToken.token}")
            tokenDao.deleteTokenByName("Anonymous")
            Log.d("TokenManager", "[ensureAnonymousToken] Старый анонимный токен удалён из БД")
            tokenDao.insertToken(Token("Anonymous", anonymousToken.token))
            Log.d("TokenManager", "[ensureAnonymousToken] Новый анонимный токен сохранён в БД")
            updateTokenEverywhere(anonymousToken.token)
        } catch (e: Exception) {
            Log.e("TokenManager", "[ensureAnonymousToken] Ошибка: ${e.message}", e)
            if (retryAttempt < MAX_RETRY_ATTEMPTS) {
                ensureAnonymousToken(retryAttempt + 1)
            } else {
                throw e
            }
        }
    }

    suspend fun login(username: String, token: String) {
        withContext(Dispatchers.IO) {
            Log.d("TokenManager", "[login] Попытка логина пользователя: $username, token: $token")
            if (validateAndUpdateToken(token, username)) {
                tokenDao.deleteTokenByName("Anonymous")
                Log.d("TokenManager", "[login] Анонимный токен удалён из БД")
                tokenDao.insertToken(Token(username, token))
                Log.d("TokenManager", "[login] Токен пользователя $username сохранён в БД")
                updateTokenEverywhere(token) 
            } else {
                Log.e("TokenManager", "[login] Не удалось валидировать токен пользователя $username")
                throw IllegalStateException("Невалидный токен пользователя")
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenDao.getAllTokens().forEach { token ->
                tokenDao.deleteToken(token)
            }
            ensureAnonymousToken()
            val anonToken = tokenDao.getTokenByName("Anonymous")?.token
            if (anonToken != null) updateTokenEverywhere(anonToken) 
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

    suspend fun isBuildingOwner(buildingId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = getCurrentToken()
                val username = getCurrentUsername()
                Log.d("TokenManager", "[isBuildingOwner] Проверка владельца для buildingId=$buildingId, token=$token, username=$username")
                if (token == null) {
                    Log.d("TokenManager", "[isBuildingOwner] Нет токена, возвращаем false")
                    return@withContext false
                }
                try {
                    val parts = token.split(".")
                    if (parts.size == 3) {
                        val payload = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
                        Log.d("TokenManager", "[isBuildingOwner] JWT payload: ${String(payload)}")
                    }
                } catch (e: Exception) {
                    Log.e("TokenManager", "[isBuildingOwner] Ошибка декодирования payload: ${e.message}")
                }
                val result = ApiFactory.apiService.isBuildingOwner(buildingId)
                Log.d("TokenManager", "[isBuildingOwner] Результат: $result для buildingId=$buildingId, username=$username")
                result
            } catch (e: Exception) {
                Log.e("TokenManager", "[isBuildingOwner] Ошибка: ${e.message}", e)
                false
            }
        }
    }
}
