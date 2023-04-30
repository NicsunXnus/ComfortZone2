package com.example.appv2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    private val _openAIKey = MutableLiveData<String>()
    private val _elevenLabsApiKey = MutableLiveData<String>()
    //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
    val openAIKey: LiveData<String> get() = _openAIKey
    //a4d7726f7a83a1942e92ce4c0a283be9
    val elevenLabsApiKey: LiveData<String> get() = _elevenLabsApiKey

    private val _systemMessageRole = MutableLiveData<String>()
    private val _systemMessageContent = MutableLiveData<String>()
    val systemMessageRole: LiveData<String> get() = _systemMessageRole
    val systemMessageContent: LiveData<String> get() = _systemMessageContent

    fun setOpenAIKey(key: String) {
        _openAIKey.value = key
    }
    fun setElevenLabsAIKey(key: String) {
        _openAIKey.value = key
    }
    fun setRole(key: String) {
        _systemMessageRole.value = key
    }
    fun setContent(key: String) {
        _systemMessageContent.value = key
    }
}