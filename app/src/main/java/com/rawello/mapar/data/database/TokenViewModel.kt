package com.rawello.mapar.data.database

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rawello.mapar.data.models.Token
import com.rawello.mapar.data.network.ApiFactory
import kotlinx.coroutines.launch
import javax.inject.Inject

class TokenViewModel @Inject constructor(private val tokenDao: TokenDao) : ViewModel() {

    fun isDbEmpty(): Boolean {
        var isEmpty = true
        viewModelScope.launch {
            isEmpty = tokenDao.isDbEmpty() == 0
        }
        return isEmpty
    }

    fun addToken(name: String, token: String) {
        viewModelScope.launch {
            val existingToken = tokenDao.getTokenByName(name)
            if (existingToken != null) {
                tokenDao.updateToken(Token(name, token))
            } else {
                tokenDao.insertToken(Token(name, token))
            }
        }
    }

    fun deleteToken(name: String) {
        viewModelScope.launch {
            tokenDao.deleteTokenByName(name)
        }
    }

    fun getAllTokens(): List<Token> {
        var tokens: List<Token> = emptyList()
        viewModelScope.launch {
            tokens = tokenDao.getAllTokens()
        }
        return tokens
    }

    suspend fun isAnonymousUser(): Boolean {
        val token = tokenDao.getTokenByName("Anonymous")
        return token != null
    }

    suspend fun isBuildingOwner(buildingId: Long): Boolean {
        try {
            // Проверяем наличие анонимного пользователя
            val anonymousToken = tokenDao.getTokenByName("Anonymous")
            if (anonymousToken != null) {
                return false
            }

            // Получаем токен авторизованного пользователя
            val userToken = tokenDao.getAllTokens().find { it.name != "Anonymous" }?.token
            if (userToken != null) {
                try {
                    Log.d("TokenViewModel", "Checking if user is building owner for buildingId: $buildingId")
                    return ApiFactory.apiService.isBuildingOwner(buildingId)
                } catch (e: Exception) {
                    Log.e("TokenViewModel", "Error checking building ownership: ", e)
                    return false
                }
            }

            return false
        } catch (e: Exception) {
            Log.e("TokenViewModel", "Error in isBuildingOwner: ", e)
            return false
        }
    }

    fun logout() {
        viewModelScope.launch {
            val anonymousToken = ApiFactory.apiService.getAnonymousToken()
            tokenDao.deleteTokenByName("Anonymous")
            // Удаляем все существующие токены пользователей
            tokenDao.getAllTokens().forEach { token ->
                if (token.name != "Anonymous") {
                    tokenDao.deleteToken(token)
                }
            }
            // Добавляем новый анонимный токен
            tokenDao.insertToken(Token("Anonymous", anonymousToken.token))
        }
    }

    fun getCurrentUser(): String? {
        var username: String? = null
        viewModelScope.launch {
            val tokens = tokenDao.getAllTokens()
            username = tokens.find { it.name != "Anonymous" }?.name
        }
        return username
    }

    suspend fun hasUserToken(): Boolean {
        return tokenDao.getAllTokens().any { it.name != "Anonymous" }
    }

    suspend fun updateAnonymousToken() {
        if (!hasUserToken()) {
            val anonymousToken = ApiFactory.apiService.getAnonymousToken()
            tokenDao.deleteTokenByName("Anonymous")
            tokenDao.insertToken(Token("Anonymous", anonymousToken.token))
        }
    }
}