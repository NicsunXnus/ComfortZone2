package com.example.appv2.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.appv2.R
import com.example.appv2.SharedViewModel
import com.example.appv2.api.*
import com.example.appv2.databinding.FragmentHomeBinding

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.*
import android.view.inputmethod.InputMethodManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.io.File
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.annotation.RequiresApi


import com.airbnb.lottie.LottieAnimationView
import java.time.LocalDateTime

import com.google.gson.*
import java.lang.reflect.Type
import java.time.format.DateTimeFormatter
import java.util.*

class HomeFragment : Fragment(), RecognitionListener {
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var spinnerVoice: Spinner
    private lateinit var chatContainer: LinearLayout
    private lateinit var remainingCharactersTextView: TextView
    private lateinit var fab: FloatingActionButton
    private var _binding: FragmentHomeBinding? = null
    private lateinit var sharedViewModel: SharedViewModel
    private var messagesList = mutableListOf<Message>()
    private lateinit var buttonGetVoices: Button
    private lateinit var manageSessionsFab: FloatingActionButton
    private lateinit var saveChatButton: FloatingActionButton
    private lateinit var voiceToTextButton: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private var handler: Handler? = null

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {
        showTypingAnimation2(false)
        Toast.makeText(requireContext(),getErrorText(error),Toast.LENGTH_SHORT).show()
    }
    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }

    override fun onResults(results: Bundle?) {
        showTypingAnimation2(true)
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            editTextMessage.append(" " + matches[0])
        }
        showTypingAnimation2(false)
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        editTextMessage = root.findViewById(R.id.editTextMessage)
        buttonSubmit = root.findViewById(R.id.buttonSubmit)
        spinnerVoice = root.findViewById(R.id.spinnerVoice)
        chatContainer = root.findViewById(R.id.chatMessagesContainer)
        buttonGetVoices = root.findViewById(R.id.get_voices_button)
        voiceToTextButton = root.findViewById(R.id.voice_to_text)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        remainingCharactersTextView = root.findViewById(R.id.remaining_characters)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer.setRecognitionListener(this)

        voiceToTextButton.setOnClickListener {

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            speechRecognizer.startListening(intent)
            Toast.makeText(requireContext(),"Recording",Toast.LENGTH_SHORT).show()
        }

        updateRemainingCharacters()

        chatContainer.setOnLongClickListener {
            Toast.makeText(requireContext(), "The Chat Box", Toast.LENGTH_SHORT).show()
            true
        }
        val onClickListener = View.OnClickListener { view ->
            // Hide keyboard
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)

            // Start loader to check parameters...
        }
        chatContainer.setOnClickListener(onClickListener)

        fab = root.findViewById(R.id.fab)
        fab.setOnClickListener {
            clearChat()
            val snackbar = Snackbar.make(root, "Chat cleared", Snackbar.LENGTH_SHORT)
            snackbar.view.setOnClickListener {
                snackbar.dismiss()
            }
            snackbar.show()
        }
        fab.setOnLongClickListener {
            Toast.makeText(requireContext(), "Clears chat", Toast.LENGTH_SHORT).show()
            true // Return true to indicate the long click event is consumed
        }

        sharedViewModel.initCharacterCount(requireContext())

        manageSessionsFab = root.findViewById(R.id.manageSessionsFab)
        manageSessionsFab.setOnClickListener { showSessionOptionsDialog() }

        CoroutineScope(Dispatchers.Main).launch {
            val apiKey = sharedViewModel.elevenLabsApiKey.value ?: "No API Key"

            try {
                val narrateApiService = Retrofit.Builder()
                    .baseUrl("https://api.elevenlabs.io/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(getVoicesAPIService::class.java)
                val response = narrateApiService.getVoices(apiKey)
                if (response.isSuccessful) {
                    val gson = Gson()
                    val responseBodyString  = response.body()?.string()
                    val voiceResponse = gson.fromJson(responseBodyString, VoiceResponse::class.java)
                    val voiceNames = voiceResponse.voices.map { "${it.name}:${it.voice_id}" }

                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voiceNames)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerVoice.adapter = adapter
                } else {
                    Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error during API call: ${e.message}")
            }
        }

        // Set up the submit button
        buttonSubmit.setOnClickListener {
            submitMessage()
        }
        buttonSubmit.setOnLongClickListener {
            Toast.makeText(requireContext(), "Sends chat to the left of the screen", Toast.LENGTH_SHORT).show()
            true
        }

        saveChatButton = root.findViewById(R.id.fab_save_chat)
        saveChatButton.setOnClickListener {
            saveCurrentChat()
        }

        buttonGetVoices.setOnClickListener {
            // Your logic to fetch the voices and display them
            CoroutineScope(Dispatchers.Main).launch {
                val apiKey = sharedViewModel.elevenLabsApiKey.value ?: "No API Key"

                try {
                    val narrateApiService = Retrofit.Builder()
                        .baseUrl("https://api.elevenlabs.io/")
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(getVoicesAPIService::class.java)
                    val response = narrateApiService.getVoices(apiKey)
                    if (response.isSuccessful) {
                        val gson = Gson()
                        val responseBodyString  = response.body()?.string()
                        val voiceResponse = gson.fromJson(responseBodyString, VoiceResponse::class.java)
                        val voiceNames = voiceResponse.voices.map { "${it.name}:${it.voice_id}" }

                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, voiceNames)
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerVoice.adapter = adapter
                    } else {
                        Toast.makeText(requireContext(), "Couldnt retrieve voice list", Toast.LENGTH_SHORT).show()
                        Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error during API call: ${e.message}")
                }
            }
        }

        buttonGetVoices.setOnLongClickListener {
            Toast.makeText(requireContext(), "Gets all the voices from the voicelab", Toast.LENGTH_SHORT).show()
            true
        }

        val changeSystemMessageButton: Button = root.findViewById(R.id.changeSystemMessageButton)
        changeSystemMessageButton.setOnClickListener {
            showChangeSystemMessageDialog()
        }
        changeSystemMessageButton.setOnLongClickListener {
            Toast.makeText(requireContext(), "Change the role of ChatGPT", Toast.LENGTH_SHORT).show()
            true
        }

        editTextMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // EditText is focused, make FABs transparent
                fab.alpha = 0.3f
                manageSessionsFab.alpha = 0.3f
                saveChatButton.alpha = 0.3f
            } else {
                // EditText lost focus, make FABs fully opaque
                fab.alpha = 1f
                manageSessionsFab.alpha = 1f
                saveChatButton.alpha = 1f
            }
        }

        return root
    }

    class LocalDateTimeSerializer : JsonSerializer<LocalDateTime> {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return JsonPrimitive(src?.format(formatter))
        }
    }

    class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return LocalDateTime.parse(json?.asString, formatter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveChatHistory() {
        val sharedPreferences = requireActivity().getSharedPreferences("chat_history", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
            .create()
        val json = gson.toJson(sharedViewModel.savedChats.value)
        editor.putString("chat_history", json)
        editor.apply()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadChatHistory(){
        val json = requireActivity().getSharedPreferences("chat_history", Context.MODE_PRIVATE).getString("chat_history", null)
        if (json != null) {
            sharedViewModel.clearHistory()
            val type = object : TypeToken<MutableList<ChatHistoryItem>>() {}.type
            val gson = GsonBuilder()
                .registerTypeAdapter(LocalDateTime::class.java,
                    HomeFragment.LocalDateTimeDeserializer()
                )
                .create()
            sharedViewModel._savedChats.value = gson.fromJson<MutableList<ChatHistoryItem>>(json, type)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveCurrentChat() {
        // Save the chat history
        val chatHistory =
            sharedViewModel.currentSession.value?.name?.let { ChatHistoryItem(title = it, messages = messagesList.toList(),
                LocalDateTime.now()) }
        if (chatHistory != null) {
            sharedViewModel.addSavedChat(chatHistory)
        }
        // Show a message that the chat was saved
        Toast.makeText(requireContext(), "Chat saved.", Toast.LENGTH_SHORT).show()
    }

    /*    fun saveChatSessions() {
        val sharedPreferences = requireActivity().getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
        val sessionsJson = Gson().toJson(sharedViewModel.chatSessions.value)
        sharedPreferences.edit().putString("sessions", sessionsJson).apply()
    }

    // Call this method to load the chat sessions from SharedPreferences
    fun loadChatSessions() {
        val sessionsJson = requireActivity().getSharedPreferences("chat_sessions", Context.MODE_PRIVATE).getString("sessions", null)
        if (sessionsJson != null) {
            sharedViewModel.clearChatSessions()
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val sessions = Gson().fromJson<List<ChatSession>>(sessionsJson, type)
            for (i in sessions.indices) {
                sharedViewModel.addChatSession(sessions[i].name,sessions[i].context,sessions[i].messages)
            }
        }
    }*/

    /*private fun saveMessagesList(messagesList: List<Message>) {
        val sharedPreferences = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(messagesList)
        editor.putString("messagesList", json)
        editor.apply()
    }*/

   /*private fun loadMessagesList(): MutableList<Message> {
        val sharedPreferences = requireActivity().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("messagesList", null)
        return if (json == null) {
            mutableListOf()
        } else {
            gson.fromJson(json, object : TypeToken<MutableList<Message>>() {}.type)
        }
    }*/

    private fun showSessionOptionsDialog() {
        val items = arrayOf("Save current session", "Load another session")
        AlertDialog.Builder(requireContext())
            .setTitle("Manage Sessions")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> saveCurrentSession()
                    1 -> loadAnotherSession()
                }
            }
            .show()
    }
    private fun saveCurrentSession() {
        val editText = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Save Current Session")
            .setMessage("Enter a name for the session:")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val sessionName = editText.text.toString()
                val sessionMessagesList = messagesList.toList()
                sharedViewModel.addChatSession(sessionName, sharedViewModel.systemMessageContent.value ?: "",sessionMessagesList)
            }
            .setNeutralButton("Update") {  _, _ ->
                val sessionName = sharedViewModel.currentSession.value?.name
                val sessionMessagesList = messagesList.toList()
                if (sessionName != null) {
                    sharedViewModel.updateChatSession(sessionName, sharedViewModel.systemMessageContent.value
                        ?: "", sessionMessagesList)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /*private fun loadAnotherSession() {
        val sessions = sharedViewModel.chatSessions.value?.map { it.name }?.toTypedArray() ?: emptyArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Load Another Session")
            .setItems(sessions) { _, which ->
                sharedViewModel.loadChatSession(sessions[which])
                clearChat()
                messagesList = sharedViewModel.currentSession.value?.messages?.toMutableList() ?: mutableListOf<Message>()
                sharedViewModel.setContent(sharedViewModel.currentSession.value?.context?:"")
                val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_system_message, null)
                val systemMsgStore = dialogView.findViewById<TextView>(R.id.systemMsgDisplay)
                systemMsgStore.text = sharedViewModel.currentSession.value?.context?:""
                //Adds back the chat messages
                for (i in messagesList.indices) {
                    if (i % 2 != 0) {
                        addMessageToChatContainer(messagesList[i].content, false)
                    } else {
                        addMessageToChatContainer(messagesList[i].content, true)
                    }
                }
            }
            .show()

    }*/

    private fun loadAnotherSession() {
        val sessions = sharedViewModel.chatSessions.value?.map { it.name }?.toMutableList() ?: mutableListOf<String>()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Load Another Session")

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_load_sessions, null)
        val sessionsListView = dialogView.findViewById<ListView>(R.id.sessions_list)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sessions)
        sessionsListView.adapter = adapter

        sessionsListView.setOnItemClickListener { _, _, position, _ ->
            sharedViewModel.loadChatSession(sessions[position])
            clearChat()
            messagesList = sharedViewModel.currentSession.value?.messages?.toMutableList() ?: mutableListOf<Message>()
            sharedViewModel.setContent(sharedViewModel.currentSession.value?.context?:"")
            //Adds back the chat messages
            for (i in messagesList.indices) {
                if (i % 2 != 0) {
                    addMessageToChatContainerNoAnimation(messagesList[i].content, false)
                } else {
                    addMessageToChatContainerNoAnimation(messagesList[i].content, true)
                }
            }
            //Adds Context
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_system_message, null)
            val systemMsgStore = dialogView.findViewById<TextView>(R.id.systemMsgDisplay)
            systemMsgStore.text = sharedViewModel.currentSession.value?.context

        }

        sessionsListView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Session")
                .setMessage("Do you want to delete this session?")
                .setPositiveButton("Yes") { _, _ ->
                    sharedViewModel.removeChatSession(sessions[position])
                    adapter.remove(sessions[position])
                    adapter.notifyDataSetChanged()
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        builder.setView(dialogView)
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
    /*
    private fun saveContext() {
        val sharedPreferences = requireActivity().getSharedPreferences("context", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("context",sharedViewModel.systemMessageContent.value?:"")
        editor.apply()
    }
    private fun loadContext() {
        val sharedPreferences = requireActivity().getSharedPreferences("context", Context.MODE_PRIVATE)
        val context = sharedPreferences.getString("context", "")

        if (!context.isNullOrEmpty()) {
            sharedViewModel.setContent(context)
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_system_message, null)
            val systemMsgStore = dialogView.findViewById<TextView>(R.id.systemMsgDisplay)
            systemMsgStore.text = context
        }
    }
    */

    // Call this method to save the chat sessions to SharedPreferences
    fun saveChatSessions() {
        val sharedPreferences = requireActivity().getSharedPreferences("chat_sessions", Context.MODE_PRIVATE)
        val sessionsJson = Gson().toJson(sharedViewModel.chatSessions.value)
        sharedPreferences.edit().putString("sessions", sessionsJson).apply()
    }

    // Call this method to load the chat sessions from SharedPreferences
    fun loadChatSessions() {
        val sessionsJson = requireActivity().getSharedPreferences("chat_sessions", Context.MODE_PRIVATE).getString("sessions", null)
        if (sessionsJson != null) {
            sharedViewModel.clearChatSessions()
            val type = object : TypeToken<List<ChatSession>>() {}.type
            val sessions = Gson().fromJson<List<ChatSession>>(sessionsJson, type)
            for (i in sessions.indices) {
                sharedViewModel.addChatSession(sessions[i].name,sessions[i].context,sessions[i].messages)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPause() {
        super.onPause()
        chatContainer.removeAllViews()
        //saveMessagesList(messagesList)
        //saveContext()
        saveChatSessions()
        saveChatHistory()
        val narrateIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.narrateIndicator)
        val typingIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.typingIndicator2)
        val handsIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.handsTyping)
        val currentSession = requireActivity().findViewById<TextView>(R.id.session_placeholder)
        narrateIndicator.visibility = View.GONE
        typingIndicator.visibility = View.GONE
        handsIndicator.visibility = View.GONE
        currentSession.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadChatSessions()
        loadChatHistory()
        messagesList.clear()
        //messagesList.addAll(loadMessagesList())
        //loadContext()
        //Adds back the chat messages
        /*for (i in messagesList.indices) {
            if (i % 2 != 0) {
                addMessageToChatContainerNoAnimation(messagesList[i].content, false)
            } else {
                addMessageToChatContainerNoAnimation(messagesList[i].content, true)
            }
        }*/
        val narrateIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.narrateIndicator)
        val typingIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.typingIndicator2)
        val handsIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.handsTyping)
        val currentSession = requireActivity().findViewById<TextView>(R.id.session_placeholder)
        narrateIndicator.visibility = View.VISIBLE
        typingIndicator.visibility = View.VISIBLE
        handsIndicator.visibility = View.VISIBLE
        currentSession.visibility = View.VISIBLE
    }

    private fun showChangeSystemMessageDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_system_message, null)

        val contentEditText = dialogView.findViewById<EditText>(R.id.contentEditText)
        val systemMsgStore = dialogView.findViewById<TextView>(R.id.systemMsgDisplay)
        systemMsgStore.text = sharedViewModel?.systemMessageContent?.value ?: ""
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Change System Message")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = contentEditText.text.toString().trim()
                sharedViewModel.setContent(newContent)
                Toast.makeText(requireContext(),"System personality updated!",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
    }

    private fun animateTextResponse(textView: TextView, message: String, onAnimationComplete: (() -> Unit)? = null) {
        fab.alpha = 0.3f
        manageSessionsFab.alpha = 0.3f
        saveChatButton.alpha = 0.3f
        var index = 0
        val animDuration = 40L // Duration for each character animation in milliseconds
        handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (index < message.length) {
                    textView.text = textView.text.toString() + message[index]
                    index++
                    handler?.postDelayed(this, animDuration)
                } else {
                    onAnimationComplete?.invoke()
                }
            }
        }
        handler?.post(runnable)
    }

    private fun typingAnimation(show : Boolean) {
        val typingIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.handsTyping)
        //typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            typingIndicator.playAnimation()
        } else {
            typingIndicator.cancelAnimation()
        }
    }

    private fun submitMessage() {
        val message = editTextMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            addMessageToChatContainer(message, true)

            // Clear the input field
            editTextMessage.setText("")

            // Fetch a reply from the ChatGPT API
            CoroutineScope(Dispatchers.Main).launch {
                sharedViewModel.openAIKey.value?.let { apiKey ->
                    showTypingAnimation2(true)
                    val chatGptResponse = fetchChatGptResponse(message, apiKey)
                    chatGptResponse?.let { response ->
                        val reply = response.choices.firstOrNull()?.message?.content?.trim()
                        reply?.let {
                            typingAnimation(true)
                            addMessageToChatContainer(it, false)
                            //sharedViewModel.addCharacterCount(countCharacters(it))
                            //sharedViewModel.characterCount.value?.let { it1 -> setCharacterCount(it1) }
                            //addMessageToChatContainer("", false)
                            val lastSystemResponse = Message("system", reply)
                            messagesList.add(lastSystemResponse)
                            val voiceName =
                                spinnerVoice.selectedItem.toString().split(":")[1].trim()
                            val narrateApiKey = sharedViewModel.elevenLabsApiKey.value
                                ?: "No Key"
                            if (narrateApiKey == "No Key") {
                                Toast.makeText(
                                    requireContext(),
                                    "No 11Lab API key found, please set an API key.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showNarrateAnimation(true)
                            generateVoiceOutput(it, voiceName, narrateApiKey) {
                                showNarrateAnimation(false)
                            }
                        }
                    }
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "No OpenAI API key found, please set an API key.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                /*?: run {
                    // Handle the case when there's no API key
                    Toast.makeText(requireContext(), "No API key found, please set an API key. Using a backup api key now", Toast.LENGTH_SHORT).show()
                    val chatGptResponse = fetchChatGptResponse(message, "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")
                    chatGptResponse?.let { response ->
                        val reply = response.choices.firstOrNull()?.message?.content?.trim()
                        reply?.let {
                            addMessageToChatContainer(it, false)
                            // Add the previous system response, if available
                            val lastSystemResponse = Message("system",reply)
                            messagesList.add(lastSystemResponse)
                            // Add this code inside the submitMessage() function, right after the line `addMessageToChatContainer(it, false)`
                            val voiceName = spinnerVoice.selectedItem.toString().split(":")[1].trim()
                            val narrateApiKey = sharedViewModel.elevenLabsApiKey.value ?: "a4d7726f7a83a1942e92ce4c0a283be9"
                            generateVoiceOutput(it, voiceName, narrateApiKey)
                        }
                    }
                }
            }*/
            }
            updateRemainingCharacters()
        }
    }

    private fun getCharacterCount(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("character_count", 0)
    }

    private fun setCharacterCount(count: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("character_count", count).apply()
    }

    private fun updateRemainingCharacters() {
        CoroutineScope(Dispatchers.Main).launch {
            sharedViewModel.elevenLabsApiKey.value?.let { apiKey ->
                try {
                    val apiService = Retrofit.Builder()
                        .baseUrl("https://api.elevenlabs.io/")
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(getCharLimit::class.java)

                    val response = apiService.getCharLim(apiKey)

                    if (response.isSuccessful) {
                        val gson = Gson()
                        val userInfo = response.body()?.string()
                        val userInfoResponse = gson.fromJson(userInfo, GetUserResponse::class.java)
                        val remainingCharacters =  userInfoResponse.subscription.character_limit - getCharacterCount()

                        remainingCharactersTextView.text = "Remaining characters: $remainingCharacters"
                    } else {
                        Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error during API call: ${e.message}")
                }
            } ?: run {
                // Handle the case when there's no API key
                Toast.makeText(requireContext(), "No 11Lab API key found, please set an API key.", Toast.LENGTH_SHORT).show()
                /*try {
                    val apiService = Retrofit.Builder()
                        .baseUrl("https://api.elevenlabs.io/")
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(getCharLimit::class.java)

                    val response = apiService.getCharLim("a4d7726f7a83a1942e92ce4c0a283be9")

                    if (response.isSuccessful) {
                        val gson = Gson()
                        val userInfo = response.body()?.string()
                        val userInfoResponse = gson.fromJson(userInfo, GetUserResponse::class.java)
                        val remainingCharacters =  userInfoResponse.subscription.character_limit

                        remainingCharactersTextView.text = "Remaining characters: $remainingCharacters"
                    } else {
                        Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error during API call: ${e.message}")
                }*/
            }
        }
    }

    /*private fun addMessageToChatContainer(message: String, isUserMessage: Boolean) {
        val textView = TextView(context).apply {
            text = message
            textSize = 18f
            setBackgroundResource(if (isUserMessage) R.drawable.speech_bubble_background else R.drawable.speech_bubble_background_reply)
            setPadding(16, 8, 16, 8) // Add padding around the text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8) // Add margins between messages
                gravity = if (isUserMessage) Gravity.START else Gravity.END
            }
        }
        chatContainer.addView(textView)
    }*/
    private fun showNarrateAnimation(show: Boolean) {
        val narrateIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.narrateIndicator)
        //typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            narrateIndicator.playAnimation()
        } else {
            narrateIndicator.cancelAnimation()
        }
    }
    private fun showTypingAnimation2(show: Boolean) {
        val typingIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.typingIndicator2)
        //typingIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            typingIndicator.playAnimation()
        } else {
            typingIndicator.cancelAnimation()
        }
    }

    private fun addMessageToChatContainer(message: String, isUserMessage: Boolean) {
        val textView = TextView(context).apply {
            text = ""
            textSize = 18f
            setTextIsSelectable(true)
            setBackgroundResource(if (isUserMessage) R.drawable.speech_bubble_background else R.drawable.speech_bubble_background_reply)
            setPadding(16, 8, 16, 8) // Add padding around the text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8) // Add margins between messages
                gravity = if (isUserMessage) Gravity.START else Gravity.END
            }
        }
        chatContainer.addView(textView)

        if (isUserMessage) {
            textView.text = message
        } else {
            animateTextResponse(textView, message) {
                showTypingAnimation2(false)
                typingAnimation(false)
                fab.alpha = 1f
                manageSessionsFab.alpha = 1f
                saveChatButton.alpha = 1f
            }

        }
    }

    private fun addMessageToChatContainerNoAnimation(message: String, isUserMessage: Boolean) {
        val textView = TextView(context).apply {
            text = ""
            textSize = 18f
            setTextIsSelectable(true)
            setBackgroundResource(if (isUserMessage) R.drawable.speech_bubble_background else R.drawable.speech_bubble_background_reply)
            setPadding(16, 8, 16, 8) // Add padding around the text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(8, 8, 8, 8) // Add margins between messages
                gravity = if (isUserMessage) Gravity.START else Gravity.END
            }
        }
        chatContainer.addView(textView)

        textView.text = message

    }
    fun countCharacters(input: String): Int {
        return input.length
    }

    fun clearChat() {
        chatContainer.removeAllViews()
        messagesList.clear()
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) // Increase the timeout duration
        .readTimeout(120, TimeUnit.SECONDS) // Increase the timeout duration
        .writeTimeout(120, TimeUnit.SECONDS) // Increase the timeout duration
        .build()

    private val chatGptService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private suspend fun fetchChatGptResponse(message: String, apiKey: String): ChatGptResponse? {
        // Add the user message to the message list
        messagesList.add(Message(role = "user", content = message))

        val requestBody = ChatGptRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                Message(role = "system", content = (if (sharedViewModel.systemMessageContent.value.isNullOrBlank()) "You are ChatGPT, a large language model" else sharedViewModel.systemMessageContent.value) + ". Please only answer the latest message in the conversation."
                )
            )   + messagesList // Add the message list to the request
        )

        return try {
            val response = chatGptService.generateChatGptResponse(requestBody, "Bearer $apiKey")

            if (response.isSuccessful) {
                Log.d("HomeFragment","Success, ${response.body()}")
                Log.d("HomeFragment","code: ${response.code()}, errorBody: ${response.errorBody()}")
                response.body()
            } else {
                Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                null
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error during API call: ${e.message}")
            null
        }
    }

    // Add this in the class HomeFragment, right below the chatGptService declaration
    private val narrateApiService: NarrateApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.elevenlabs.io/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NarrateApiService::class.java)
    }

    private suspend fun generateVoiceOutput(text: String, voiceName: String, apiKey: String, callback: ()->Unit) {
        if (sharedViewModel.useElevenLabApiKey.value == true) {
            val voiceSettings = VoiceSettings(stability = 0.6f, similarity_boost = 0.8f)
            val requestBody = NarrateRequest(text, voiceSettings)
            try {
                val response = narrateApiService.generateVoice(voiceName, apiKey, requestBody)
                if (response.isSuccessful) {
                    sharedViewModel.addCharacterCount(countCharacters(text))
                    sharedViewModel.characterCount.value?.let { it1 -> setCharacterCount(it1) }
                    val audioBytes = response.body()?.bytes()
                    audioBytes?.let {
                        val tempFile =
                            File.createTempFile("tempAudio", "mp3", requireContext().cacheDir)
                                .apply {
                                    writeBytes(it)
                                    deleteOnExit()
                                }
                        val mediaPlayer = MediaPlayer().apply {
                            setDataSource(requireContext(), Uri.fromFile(tempFile))
                            prepare()
                            setOnCompletionListener {
                                callback()
                            }
                            start()
                        }
                    }
                } else {
                    Log.e(
                        "HomeFragment",
                        "Unsuccessful Narrate API response, code: ${response.code()}, errorBody: ${response.errorBody()}"
                    )
                    Toast.makeText(
                        requireContext(),
                        "Unsuccessful Narrate API response, code: ${response.code()}, errorBody: ${response.errorBody()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNarrateAnimation(false)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error during Narrate API call: ${e.message}")
            }
        } else {
            val voiceIndicator =  requireActivity().findViewById<LottieAnimationView>(R.id.narrateIndicator)
            voiceIndicator.cancelAnimation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer.destroy()
        handler?.removeCallbacksAndMessages(null)
        handler = null
        _binding = null
    }
}