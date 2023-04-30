package com.example.appv2.ui.home

import android.app.AlertDialog
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
import android.view.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

import com.google.gson.Gson
import java.io.File

class HomeFragment : Fragment() {
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSubmit: Button
    private lateinit var spinnerVoice: Spinner
    private lateinit var chatContainer: LinearLayout
    private lateinit var remainingCharactersTextView: TextView
    private lateinit var fab: FloatingActionButton
    private var _binding: FragmentHomeBinding? = null
    private lateinit var sharedViewModel: SharedViewModel
    private val messagesList = mutableListOf<Message>()
    private lateinit var buttonGetVoices: Button


    // This property is only valid between onCreateView and
    // onDestroyView.
    //private val binding get() = _binding!!

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
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        remainingCharactersTextView = root.findViewById(R.id.remaining_characters)

        updateRemainingCharacters()

        fab = root.findViewById(R.id.fab)
        fab.setOnClickListener {
            clearChat()
            val snackbar = Snackbar.make(root, "Chat cleared", Snackbar.LENGTH_SHORT)
            snackbar.view.setOnClickListener {
                snackbar.dismiss()
            }
            snackbar.show()
        }

        CoroutineScope(Dispatchers.Main).launch {
            val apiKey = sharedViewModel.elevenLabsApiKey.value ?: "a4d7726f7a83a1942e92ce4c0a283be9"

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

        buttonGetVoices.setOnClickListener {
            // Your logic to fetch the voices and display them
            CoroutineScope(Dispatchers.Main).launch {
                val apiKey = sharedViewModel.elevenLabsApiKey.value ?: "a4d7726f7a83a1942e92ce4c0a283be9"

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
                        Toast.makeText(requireContext(), "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}", Toast.LENGTH_SHORT).show()
                        Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "\"Error during API call: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("HomeFragment", "Error during API call: ${e.message}")
                }
            }
        }

        val changeSystemMessageButton: Button = root.findViewById(R.id.changeSystemMessageButton)
        changeSystemMessageButton.setOnClickListener {
            showChangeSystemMessageDialog()
        }

        return root
    }

    private fun showChangeSystemMessageDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_system_message, null)

        val contentEditText = dialogView.findViewById<EditText>(R.id.contentEditText)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Change System Message")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newContent = contentEditText.text.toString().trim()
                if (newContent.isNotEmpty()) {
                    sharedViewModel.setContent(newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        alertDialog.show()
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
                    val chatGptResponse = fetchChatGptResponse(message, apiKey)
                    chatGptResponse?.let { response ->
                        val reply = response.choices.firstOrNull()?.message?.content?.trim()
                        reply?.let {
                            addMessageToChatContainer(it, false)
                            // Add this code inside the submitMessage() function, right after the line `addMessageToChatContainer(it, false)`
                            val voiceName = spinnerVoice.selectedItem.toString().split(":")[1].trim()
                            val narrateApiKey = sharedViewModel.elevenLabsApiKey.value ?: "a4d7726f7a83a1942e92ce4c0a283be9"
                            generateVoiceOutput(it, voiceName, narrateApiKey)
                        }
                    }
                } ?: run {
                    // Handle the case when there's no API key
                    Toast.makeText(requireContext(), "No API key found, please set an API key. Using a backup api key now", Toast.LENGTH_SHORT).show()
                    val chatGptResponse = fetchChatGptResponse(message, "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")
                    chatGptResponse?.let { response ->
                        val reply = response.choices.firstOrNull()?.message?.content?.trim()
                        reply?.let {
                            addMessageToChatContainer(it, false)
                            // Add this code inside the submitMessage() function, right after the line `addMessageToChatContainer(it, false)`
                            val voiceName = spinnerVoice.selectedItem.toString().split(":")[1].trim()
                            val narrateApiKey = sharedViewModel.elevenLabsApiKey.value ?: "a4d7726f7a83a1942e92ce4c0a283be9"
                            generateVoiceOutput(it, voiceName, narrateApiKey)
                        }
                    }
                }
            }
            updateRemainingCharacters()
        }
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
                        val remainingCharacters =  userInfoResponse.subscription.character_limit

                        remainingCharactersTextView.text = "Remaining characters: $remainingCharacters"
                    } else {
                        Log.e("HomeFragment", "Unsuccessful API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error during API call: ${e.message}")
                }
            } ?: run {
                // Handle the case when there's no API key
                Toast.makeText(requireContext(), "No 11Lab API key found, please set an API key. Using a backup api key now", Toast.LENGTH_SHORT).show()
                try {
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
                }
            }
        }
    }


    private fun addMessageToChatContainer(message: String, isUserMessage: Boolean) {
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
    }

    fun clearChat() {
        chatContainer.removeAllViews()
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
                Message(role = "system", content = sharedViewModel.systemMessageContent.value + ".Please only answer the latest message in the conversation." ?: "You are ChatGPT, a large language model. Please only answer the latest message in the conversation.")
            ) + messagesList // Add the message list to the request

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

    private suspend fun generateVoiceOutput(text: String, voiceName: String, apiKey: String) {
        val voiceSettings = VoiceSettings(stability = 0.6f, similarity_boost = 0.8f)
        val requestBody = NarrateRequest(text, voiceSettings)
        try {
            val response = narrateApiService.generateVoice(voiceName, apiKey, requestBody)
            if (response.isSuccessful) {
                val audioBytes = response.body()?.bytes()
                audioBytes?.let {
                    val tempFile = File.createTempFile("tempAudio", "mp3", requireContext().cacheDir).apply {
                        writeBytes(it)
                        deleteOnExit()
                    }
                    val mediaPlayer = MediaPlayer().apply {
                        setDataSource(requireContext(), Uri.fromFile(tempFile))
                        prepare()
                        start()
                    }
                }
            } else {
                Log.e("HomeFragment", "Unsuccessful Narrate API response, code: ${response.code()}, errorBody: ${response.errorBody()}")
                Toast.makeText(requireContext(),"Unsuccessful Narrate API response, code: ${response.code()}, errorBody: ${response.errorBody()}", Toast.LENGTH_SHORT).show()

            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error during Narrate API call: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}