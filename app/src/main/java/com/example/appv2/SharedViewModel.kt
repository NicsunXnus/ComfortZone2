package com.example.appv2


import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.appv2.api.Message
import com.example.appv2.ui.home.ChatHistoryItem
import com.example.appv2.ui.home.ChatSession
import com.google.gson.Gson

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

    internal val _savedChats = MutableLiveData(mutableListOf<ChatHistoryItem>())
    val savedChats: LiveData<MutableList<ChatHistoryItem>> get() = _savedChats

    private var _characterCount = MutableLiveData<Int>()
    val characterCount: LiveData<Int> get() = _characterCount

    fun addCharacterCount(count: Int) {
        if (_characterCount.value == null) {
            _characterCount.value = 0
        }
        _characterCount.value = _characterCount.value?.plus(count)
    }

    fun initCharacterCount(context: Context) {
        val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        _characterCount.value = sharedPreferences.getInt("character_count", 0)
    }
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
        _currentSession.value = ChatSession(name, context, messages)
    }

    fun updateChatSession(name: String, context: String, messages: List<Message>) {
        val session = _chatSessions.value?.find { it.name == name }
        if (session != null) {
            session.context = context
            session.messages = messages
        }
    }


    fun removeChatSession(name: String) {
        _chatSessions.value = _chatSessions.value?.filter { it.name != name }?.toMutableList()
        _currentSession.value = ChatSession("No existing session", "No context", mutableListOf<Message>())
    }

    fun loadChatSession(name: String) {
        _currentSession.value = _chatSessions.value?.find { it.name == name }
    }

    fun clearChatSessions() {
        _chatSessions.value?.clear()
    }
    fun clearHistory() {
        _savedChats.value?.clear()
    }
    fun saveChatHistory(context: Context) {
        val sharedPreferences = context.getSharedPreferences("chat_history", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(_savedChats.value)
        editor.putString("chat_history", json)
        editor.apply()
    }

    fun deleteChatHistoryItem(chatTitle: String, context: Context) {
        val updatedList = _savedChats.value?.filterNot { it.title == chatTitle }
        _savedChats.value = updatedList?.toMutableList()
        saveChatHistory(context)
    }

    fun addSavedChat(chat: ChatHistoryItem) {
        val existingItemIndex = _savedChats.value?.indexOfFirst { it.title == chat.title }

        if (existingItemIndex != null && existingItemIndex >= 0) {
            // Update the existing item
            _savedChats.value?.set(existingItemIndex, chat)
        } else {
            // Add a new item
            _savedChats.value?.add(chat)
        }
        // _savedChats.value?.add(chat)
        _savedChats.postValue(_savedChats.value)
    }
}