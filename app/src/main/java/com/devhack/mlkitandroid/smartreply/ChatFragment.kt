package com.devhack.mlkitandroid.smartreply

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.devhack.mlkitandroid.R
import java.util.*


class ChatFragment : Fragment(), ReplyChipAdapter.ClickListener {

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var inputText: TextView
    private lateinit var sendButton: Button
    private lateinit var switchUserButton: Button

    private lateinit var chatRecycler: RecyclerView
    private lateinit var chatAdapter: MessageListAdapter

    private lateinit var smartRepliesRecycler: RecyclerView
    private lateinit var chipAdapter: ReplyChipAdapter

    private lateinit var emulatedUserText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpViews(view)

        setUpRecyclers()

        listeners()

        viewModelObservers()

        val messageList = mutableListOf<Message>()
            .apply {
                add(
                    Message(
                        "Hello. How are you?",
                        false,
                        System.currentTimeMillis()
                    )
                )
            }

        viewModel.setMessages(messageList)
    }

    override fun onChipClick(chipText: String) {
        inputText.text = chipText
    }

    private fun viewModelObservers() {
        viewModel.getSuggestions().observe(
            viewLifecycleOwner,
            Observer { suggestions ->
                suggestions?.let {
                    chipAdapter.setSuggestions(suggestions)
                }
            })

        viewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
            messages?.let {
                chatAdapter.setMessages(messages)
                if (chatAdapter.itemCount > 0) {
                    chatRecycler.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        })

        viewModel.getEmulatingRemoteUser()
            .observe(viewLifecycleOwner, Observer { isEmulatingRemoteUser ->
                isEmulatingRemoteUser?.let {
                    if (isEmulatingRemoteUser) {
                        emulatedUserText.setText(R.string.chatting_as_red)
                        emulatedUserText.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.red
                            )
                        )
                    } else {
                        emulatedUserText.setText(R.string.chatting_as_blue)
                        emulatedUserText.setTextColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.blue
                            )
                        )
                    }
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun listeners() {
        chatRecycler.setOnTouchListener { touchView, _ ->
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(touchView.windowToken, 0)
            false
        }

        switchUserButton.setOnClickListener {
            chatAdapter.emulatingRemoteUser = !chatAdapter.emulatingRemoteUser
            viewModel.switchUser()
        }

        sendButton.setOnClickListener(View.OnClickListener {
            val input = inputText.text.toString()
            if (TextUtils.isEmpty(input)) {
                return@OnClickListener
            }

            viewModel.addMessage(input)
            inputText.text = ""
        })
    }

    private fun setUpViews(view: View) {
        chatRecycler = view.findViewById(R.id.chatHistory)
        emulatedUserText = view.findViewById(R.id.switchText)
        smartRepliesRecycler = view.findViewById(R.id.smartRepliesRecycler)
        inputText = view.findViewById(R.id.inputText)
        sendButton = view.findViewById(R.id.button)
        switchUserButton = view.findViewById(R.id.switchEmulatedUser)
    }

    private fun setUpRecyclers() {
        // Set up recycler view for chat messages
        val layoutManager = LinearLayoutManager(context)
        chatRecycler.layoutManager = layoutManager
        chatAdapter = MessageListAdapter()
        chatRecycler.adapter = chatAdapter

        // Set up recycler view for smart replies
        val chipManager = LinearLayoutManager(context)
        chipManager.orientation = RecyclerView.HORIZONTAL
        chipAdapter = ReplyChipAdapter(this)
        smartRepliesRecycler.layoutManager = chipManager
        smartRepliesRecycler.adapter = chipAdapter
    }

    private fun generateChatHistoryBasic() {
        val messageList = ArrayList<Message>()
        val calendar = Calendar.getInstance()

        calendar.set(Calendar.DATE, -1)
        messageList.add(Message("Hello", true, calendar.timeInMillis))

        calendar.add(Calendar.MINUTE, 10)
        messageList.add(Message("Hey", false, calendar.timeInMillis))

        viewModel.setMessages(messageList)
    }

    companion object {

        fun newInstance(): ChatFragment {
            return ChatFragment()
        }
    }
}