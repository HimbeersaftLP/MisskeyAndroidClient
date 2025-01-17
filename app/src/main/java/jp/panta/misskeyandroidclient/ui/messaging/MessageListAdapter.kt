package jp.panta.misskeyandroidclient.ui.messaging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.databinding.ItemMessageRecipientBinding
import jp.panta.misskeyandroidclient.databinding.ItemMessageSelfBinding
import jp.panta.misskeyandroidclient.ui.messaging.viewmodel.MessageViewData
import jp.panta.misskeyandroidclient.ui.messaging.viewmodel.SelfMessageViewData
import java.lang.IllegalArgumentException


class MessageListAdapter(diffUtilItemCallback: DiffUtil.ItemCallback<MessageViewData>, val lifecycleOwner: LifecycleOwner) : ListAdapter<MessageViewData, MessageListAdapter.MessageViewHolder>(diffUtilItemCallback){
    abstract class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class MessageSelfViewHolder(val binding: ItemMessageSelfBinding) : MessageViewHolder(binding.root)
    class MessageRecipientViewHolder(val binding: ItemMessageRecipientBinding) : MessageViewHolder(binding.root)

    companion object{
        private const val SELF = 0
        private const val RECIPIENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if(item is SelfMessageViewData){
            SELF
        }else{
            RECIPIENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when(viewType){
            SELF ->{
                val binding = DataBindingUtil.inflate<ItemMessageSelfBinding>(LayoutInflater.from(parent.context), R.layout.item_message_self, parent, false)
                MessageSelfViewHolder(binding)
            }
            RECIPIENT ->{
                val binding = DataBindingUtil.inflate<ItemMessageRecipientBinding>(LayoutInflater.from(parent.context), R.layout.item_message_recipient, parent, false)
                MessageRecipientViewHolder(binding)
            }
            else -> throw IllegalArgumentException("viewType not found: $viewType")
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        if(holder is MessageSelfViewHolder){
            holder.binding.message = getItem(position)
            holder.binding.lifecycleOwner = lifecycleOwner
            holder.binding.executePendingBindings()
        }else if(holder is MessageRecipientViewHolder){
            holder.binding.message = getItem(position)
            holder.binding.lifecycleOwner = lifecycleOwner
            holder.binding.executePendingBindings()
        }
    }

}