package com.example.appv2.ui.gallery

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.appv2.databinding.FragmentGalleryBinding

import android.content.Intent
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.appv2.SharedViewModel

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val openAIEditText: EditText = binding.editOpenaiApiKey
        val elevenLabEditText: EditText = binding.editElevenlabApiKey

        val submitOpenAIButton: Button = binding.buttonSubmitOpenaiApiKey
        val submitElevenLabButton: Button = binding.buttonSubmitElevenlabApiKey
        val openAIKeyTextView: TextView = binding.textViewOpenaiApiKey
        val elevenLabKeyTextView: TextView = binding.textViewElevenlabApiKey
        val characterCount: TextView = binding.elevenLabUsage
        val elevenLabSwitch : TextView = binding.elevenLabSwitch

        val clearApiKeysButton: Button = binding.clearApiKeysButton
        clearApiKeysButton.setOnClickListener {
            openAIKeyTextView.text = ""
            elevenLabKeyTextView.text = ""
            saveApiKeyToSharedPreferences("openai_api_key", "")
            saveApiKeyToSharedPreferences("elevenlab_api_key", "")
        }

        openAIEditText.setOnClickListener {
            clearApiKeysButton.visibility = View.GONE
            Toast.makeText(requireContext(),"Click the white space to bring back the clear api keys button",Toast.LENGTH_SHORT).show()
        }
        elevenLabEditText.setOnClickListener {
            clearApiKeysButton.visibility = View.GONE
            Toast.makeText(requireContext(),"Click the white space to bring back the clear api keys button",Toast.LENGTH_SHORT).show()
        }
        root.setOnClickListener {
            clearApiKeysButton.visibility = View.VISIBLE
        }

        sharedViewModel.initCharacterCount(requireContext())
        if (sharedViewModel.characterCount.value == null) {
            sharedViewModel.addCharacterCount(0)
        }
        characterCount.text = characterCount.text.toString() + sharedViewModel.characterCount.value
        loadApiKeysFromSharedPreferences()

        sharedViewModel.initElevenLabAPIkeySwitch()
        elevenLabSwitch.text = elevenLabSwitch.text.toString() + sharedViewModel.useElevenLabApiKey.value

        elevenLabSwitch.setOnLongClickListener {
            sharedViewModel.switchElevenLabApiKey()
            elevenLabSwitch.text = "ElevenLabSwitch: " + sharedViewModel.useElevenLabApiKey.value
            true
        }

        submitOpenAIButton.setOnClickListener {
            val openAIKey = binding.editOpenaiApiKey.text.toString()
            passOpenAIKey(openAIKey)
            openAIKeyTextView.text = "OpenAI API Key: $openAIKey"
            saveApiKeyToSharedPreferences("openai_api_key", openAIKey)
        }

        characterCount.setOnLongClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Character Count")
                .setMessage("Are you sure you want to delete?")
                .setPositiveButton("Yes") { _, _ ->
                    setCharacterCount(0)
                    characterCount.text = "Character Count: 0"
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        characterCount.setOnClickListener {
            val editText = EditText(requireContext())
            AlertDialog.Builder(requireContext())
                .setTitle("Change Character Count")
                .setMessage("Do you wish to edit?")
                .setView(editText)
                .setPositiveButton("Yes") { _, _ ->
                    setCharacterCount(editText.text.toString().toInt())
                    characterCount.text = "Character Count: " + editText.text.toString()
                }
                .setNegativeButton("No", null)
                .show()
            true
        }

        submitElevenLabButton.setOnClickListener {
            val elevenLabKey = binding.editElevenlabApiKey.text.toString()
            passElevenLabKey(elevenLabKey)
            elevenLabKeyTextView.text = "ElevenLab API Key: $elevenLabKey"
            saveApiKeyToSharedPreferences("elevenlab_api_key", elevenLabKey)
        }


        val openAIApiLink: TextView = binding.openaiApiLink
        val elevenLabApiLink: TextView = binding.elevenlabApiLink

        val openAIApiUrl = "https://platform.openai.com/account/api-keys"
        val elevenLabApiUrl = "https://beta.elevenlabs.io/speech-synthesis"

        val openAIClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(openAIApiUrl))
                startActivity(intent)
            }
        }
        val elevenLabClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(elevenLabApiUrl))
                startActivity(intent)
            }
        }

        openAIApiLink.apply {
            text = SpannableStringBuilder("Get OpenAI API Key").apply {
                setSpan(openAIClickableSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            movementMethod = LinkMovementMethod.getInstance()
        }

        elevenLabApiLink.apply {
            text = SpannableStringBuilder("Get ElevenLab API Key").apply {
                setSpan(elevenLabClickableSpan, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            movementMethod = LinkMovementMethod.getInstance()
        }



        return root
    }

    private fun setCharacterCount(count: Int) {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("character_count", count).apply()
    }

    private fun passOpenAIKey(openAIKey: String) {
        // Use the OpenAI API key here or pass it to another part of your app
        sharedViewModel.setOpenAIKey(openAIKey)
    }

    private fun passElevenLabKey(elevenLabKey: String) {
        // Use the ElevenLab API key here or pass it to another part of your app
        sharedViewModel.setElevenLabsAIKey(elevenLabKey)
    }

    private fun saveApiKeyToSharedPreferences(keyName: String, apiKey: String) {
        val sharedPreferences = requireActivity().getSharedPreferences("apikeys", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(keyName, apiKey)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        loadApiKeysFromSharedPreferences()
    }

    private fun loadApiKeysFromSharedPreferences() {
        val sharedPreferences = requireActivity().getSharedPreferences("apikeys", Context.MODE_PRIVATE)
        val openAIKey = sharedPreferences.getString("openai_api_key", "")
        val elevenLabKey = sharedPreferences.getString("elevenlab_api_key", "")
        val openAIKeyTextView: TextView = binding.textViewOpenaiApiKey
        val elevenLabKeyTextView: TextView = binding.textViewElevenlabApiKey

        if (!openAIKey.isNullOrEmpty()) {
            openAIKeyTextView.text = "OpenAI API Key: $openAIKey"
            passOpenAIKey(openAIKey)
        }

        if (!elevenLabKey.isNullOrEmpty()) {
            elevenLabKeyTextView.text = "ElevenLab API Key: $elevenLabKey"
            passElevenLabKey(elevenLabKey)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}