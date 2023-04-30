package com.example.appv2.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
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
import com.example.appv2.SharedViewModel


class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    //private lateinit var apiKey: String
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
        val galleryViewModel =
            ViewModelProvider(this).get(GalleryViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val editTextGallery: EditText = binding.editTextGallery

        //Remove?
        galleryViewModel.text.observe(viewLifecycleOwner) {
            //editTextGallery.text = it
        }

        val openAIEditText: EditText = binding.editOpenaiApiKey
        val elevenLabEditText: EditText = binding.editElevenlabApiKey

        openAIEditText.addTextChangedListener {
            // Handle the OpenAI API key input here
        }

        elevenLabEditText.addTextChangedListener {
            // Handle the ElevenLab API key input here
        }

        val submitOpenAIButton: Button = binding.buttonSubmitOpenaiApiKey
        val submitElevenLabButton: Button = binding.buttonSubmitElevenlabApiKey
        val openAIKeyTextView: TextView = binding.textViewOpenaiApiKey
        val elevenLabKeyTextView: TextView = binding.textViewElevenlabApiKey
        submitOpenAIButton.setOnClickListener {
            val openAIKey = binding.editOpenaiApiKey.text.toString()
            passOpenAIKey(openAIKey)
            openAIKeyTextView.text = "OpenAI API Key: $openAIKey"
        }

        submitElevenLabButton.setOnClickListener {
            val elevenLabKey = binding.editElevenlabApiKey.text.toString()
            passElevenLabKey(elevenLabKey)
            elevenLabKeyTextView.text = "ElevenLab API Key: $elevenLabKey"
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

    private fun passOpenAIKey(openAIKey: String) {
        // Use the OpenAI API key here or pass it to another part of your app
        sharedViewModel.setOpenAIKey(openAIKey)
    }

    private fun passElevenLabKey(elevenLabKey: String) {
        // Use the ElevenLab API key here or pass it to another part of your app
        sharedViewModel.setElevenLabsAIKey(elevenLabKey)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}