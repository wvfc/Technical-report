package com.soutech.relatoriotecnico.ui.maquina

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.databinding.ItemMaquinaBinding

data class MaquinaDto(
    val id: Int,
    val clientId: Int,
    val brand: String,
    val model: String,
    val serialNumber: String,
    val clientName: String? = null
)

class MaquinaAdapter(
    private val itens: List<MaquinaDto>
) : RecyclerView.Adapter<MaquinaAdapter.MaquinaViewHolder>() {

    inner class MaquinaViewHolder(val binding: ItemMaquinaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaquinaViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemMaquinaBinding.inflate(inflater, parent, false)
        return MaquinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaquinaViewHolder, position: Int) {
        val item = itens[position]
        holder.binding.tvLinha1.text = "${item.brand} / ${item.model}"
        holder.binding.tvLinha2.text = "Nº de série: ${item.serialNumber}"
        holder.binding.tvLinha3.text = "Cliente: ${item.clientName ?: "-"}"
    }

    override fun getItemCount(): Int = itens.size
}
