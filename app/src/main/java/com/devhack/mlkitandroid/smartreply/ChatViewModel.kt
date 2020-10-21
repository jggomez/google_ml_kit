package com.devhack.mlkitandroid.smartreply

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestion
import com.google.mlkit.nl.smartreply.TextMessage
import java.util.*

class ChatViewModel : ViewModel() {

    private val REMOTE_USER_ID = UUID.randomUUID().toString()

    private val suggestions = MediatorLiveData<List<SmartReplySuggestion>>()
    private val messageList = MutableLiveData<MutableList<Message>>()
    private val emulatingRemoteUser = MutableLiveData<Boolean>()
    private val smartReply = SmartReply.getClient()

    val messages: LiveData<MutableList<Message>>
        get() = messageList

    init {
        initSuggestionsGenerator()
        emulatingRemoteUser.postValue(false)
    }

    fun getSuggestions(): LiveData<List<SmartReplySuggestion>> = suggestions

    fun getEmulatingRemoteUser(): LiveData<Boolean> = emulatingRemoteUser

    internal fun setMessages(messages: MutableList<Message>) {
        clearSuggestions()
        messageList.postValue(messages)
    }

    internal fun switchUser() {
        clearSuggestions()
        emulatingRemoteUser.value?.let {
            emulatingRemoteUser.postValue(!it)
        }
    }

    internal fun addMessage(message: String) {
        var list = messageList.value
        list = list ?: ArrayList()
        emulatingRemoteUser.value?.let {
            list.add(Message(message, !it, System.currentTimeMillis()))
            clearSuggestions()
            messageList.postValue(list)
        }
    }

    private fun clearSuggestions() {
        suggestions.postValue(emptyList())
    }

    private fun initSuggestionsGenerator() {
        suggestions.addSource(emulatingRemoteUser, Observer { isEmulatingRemoteUser ->
            val list = messageList.value
            if (list == null || list.isEmpty()) {
                return@Observer
            }

            isEmulatingRemoteUser?.let {
                generateReplies(list, it)
                    .addOnSuccessListener { result -> suggestions.postValue(result) }
            }
        })

        suggestions.addSource(messageList, Observer { list ->
            val isEmulatingRemoteUser = emulatingRemoteUser.value

            if (isEmulatingRemoteUser == null || list!!.isEmpty()) {
                return@Observer
            }

            generateReplies(list, isEmulatingRemoteUser).addOnSuccessListener { result ->
                suggestions.postValue(result)
            }
        })
    }

    private fun generateReplies(
        messages: List<Message>,
        isEmulatingRemoteUser: Boolean
    ): Task<List<SmartReplySuggestion>> {

        val lastMessage = messages[messages.size - 1]

        // If the last message in the chat thread is not sent by the "other" user, don't generate
        // smart replies.
        if (lastMessage.isLocalUser && !isEmulatingRemoteUser
            || !lastMessage.isLocalUser && isEmulatingRemoteUser
        ) {
            return Tasks.forException(Exception("Not running smart reply!"))
        }

        val chatHistory = mutableListOf<TextMessage>()
        for (message in messages) {
            if (message.isLocalUser && !isEmulatingRemoteUser
                || !message.isLocalUser && isEmulatingRemoteUser
            ) {
                chatHistory.add(
                    TextMessage.createForLocalUser(
                        message.text,
                        message.timestamp
                    )
                )
            } else {
                chatHistory.add(
                    TextMessage.createForRemoteUser(
                        message.text,
                        message.timestamp,
                        REMOTE_USER_ID
                    )
                )
            }
        }

        return smartReply.suggestReplies(chatHistory)
            .continueWith { task -> task.result!!.suggestions }
    }

    override fun onCleared() {
        super.onCleared()
        smartReply.close()
    }
}