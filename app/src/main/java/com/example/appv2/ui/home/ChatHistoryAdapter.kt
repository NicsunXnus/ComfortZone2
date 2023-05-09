package com.example.appv2.ui.home
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.appv2.R
import com.example.appv2.SharedViewModel
import java.time.format.DateTimeFormatter

// Modify the input type to List<ChatHistoryItem>
class ChatHistoryAdapter(private val sharedViewModel: SharedViewModel,private val chatHistoryItems: MutableList<ChatHistoryItem>) :
    RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chatTitle: TextView = view.findViewById(R.id.chat_title)
        val timestamp: TextView = view.findViewById(R.id.chat_timestamp)
        val chatMsg: TextView = view.findViewById(R.id.chat_message)

        init {
            view.setOnClickListener {
                val clipboard = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("ChatHistoryItem", chatMsg.text)
                clipboard.setPrimaryClip(clip)
                // Show a message to the user
                Toast.makeText(itemView.context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }

            view.setOnLongClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete this chat?")
                    .setPositiveButton("Yes") { _, _ ->
                        val position = adapterPosition
                        sharedViewModel.deleteChatHistoryItem(chatHistoryItems[position].title, itemView.context)
                        chatHistoryItems.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    .setNegativeButton("No", null)
                    .show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_history_item, parent, false)
        return ViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatHistoryItem = chatHistoryItems[position]
        //val messages = chatHistoryItem.messages.joinToString(separator = "\n") { it.content }
        val messages = chatHistoryItem.messages.mapIndexed { index, message ->
            val emoji = if (index % 2 == 0) "😀" else "😎"
            "$emoji: ${message.content}\n\n"
        }.joinToString(separator = "")

        holder.chatTitle.text = "${chatHistoryItem.title}"
        holder.chatTitle.textSize = 30f
        holder.chatTitle.setTypeface(null, Typeface.BOLD)
        holder.timestamp.text = chatHistoryItem.timestamp.format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        holder.timestamp.setTypeface(null, Typeface.ITALIC)
        holder.chatMsg.text = messages
        holder.chatMsg.textSize = 22f
        holder.chatMsg.typeface = ResourcesCompat.getFont(holder.itemView.context, R.font.adventprobold)

        val colorA = ContextCompat.getColor(holder.itemView.context, R.color.colorAccent)
        val colorB = ContextCompat.getColor(holder.itemView.context, R.color.teal_200)

        if (position % 2 == 0) {
            holder.itemView.setBackgroundColor(colorA)
        } else {
            holder.itemView.setBackgroundColor(colorB)
        }
    }

    override fun getItemCount(): Int {
        return chatHistoryItems.size
    }
}

