package com.example.appv2


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appv2.api.Message
import com.example.appv2.ui.home.ChatSession

class SharedViewModel : ViewModel() {
    private val _openAIKey = MutableLiveData<String>()
    private val _elevenLabsApiKey = MutableLiveData<String>()
    //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
    val openAIKey: LiveData<String> get() = _openAIKey
    //a4d7726f7a83a1942e92ce4c0a283be9
    val elevenLabsApiKey: LiveData<String> get() = _elevenLabsApiKey

    private val _systemMessageContent = MutableLiveData<String>()
    val systemMessageContent: LiveData<String> get() = _systemMessageContent

    private val _chatSessions = MutableLiveData<MutableList<ChatSession>>(mutableListOf())
    val chatSessions: LiveData<MutableList<ChatSession>> get() = _chatSessions

    private val _currentSession = MutableLiveData<ChatSession>()
    val currentSession: LiveData<ChatSession> get() = _currentSession

    fun setOpenAIKey(key: String) {
        _openAIKey.value = key
    }
    fun setElevenLabsAIKey(key: String) {
        _elevenLabsApiKey.value = key
    }

    fun setContent(key: String) {
        _systemMessageContent.value = key
    }

    fun addChatSession(name: String, context: String, messages: List<Message>) {
        _chatSessions.value?.add(ChatSession(name, context, messages))
    }

    fun removeChatSession(name: String) {
        _chatSessions.value = _chatSessions.value?.filter { it.name != name }?.toMutableList()
    }

    fun loadChatSession(name: String) {
        _currentSession.value = _chatSessions.value?.find { it.name == name }
    }

    fun clearChatSessions() {
        _chatSessions.value?.clear()
    }

}