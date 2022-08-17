package com.suyu.globetrotter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suyu.globetrotter.database.globetrotter.Player
import com.suyu.globetrotter.databinding.PlayerListItemBinding

//  Provides template cell for each player in player list fragment
class PlayerListAdapter(private val onItemClicked: (Player) -> Unit) :
    ListAdapter<Player, PlayerListAdapter.PlayerViewHolder>(DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            return PlayerViewHolder(
                PlayerListItemBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    )
                )
            )
        }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            onItemClicked(current)
        }
        holder.bind(current)
    }

    class PlayerViewHolder(private var binding: PlayerListItemBinding) :
            RecyclerView.ViewHolder(binding.root) {

                fun bind(player: Player) {
                    binding.apply {
                        playerName.text = player.playerName
                        playerScore.text = "%.2f".format(player.currentScore)
                    }
                }
            }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Player>() {
            override fun areItemsTheSame(oldItem: Player, newItem: Player): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: Player, newItem: Player): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}