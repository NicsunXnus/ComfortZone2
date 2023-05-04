package com.example.appv2.ui.history

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import androidx.recyclerview.widget.RecyclerView
import com.example.appv2.R
import com.example.appv2.SharedViewModel
import com.example.appv2.databinding.FragmentHistoryBinding
import com.example.appv2.ui.home.ChatHistoryAdapter
import com.example.appv2.ui.home.ChatHistoryItem
import com.example.appv2.ui.home.HomeFragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.time.LocalDateTime

class ChatHistory : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private lateinit var sharedViewModel: SharedViewModel
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root
        recyclerView = binding.recyclerView
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.savedChats.observe(viewLifecycleOwner, { savedChats ->
            recyclerView.adapter = ChatHistoryAdapter(sharedViewModel,savedChats)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        })


        return root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadChatHistory()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}