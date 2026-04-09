package com.soutech.relatoriotecnico.ui.maquina

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.data.MaquinaEntity
import com.soutech.relatoriotecnico.databinding.ItemMaquinaBinding

class MaquinaAdapter(
    private val itens: List<MaquinaEntity>,
    private val mapaClientes: Map<Long, String> = emptyMap(),
    private val onExcluir: ((MaquinaEntity) -> Unit)? = null
) : RecyclerView.Adapter<MaquinaAdapter.MaquinaViewHolder>() {

    inner class MaquinaViewHolder(val binding: ItemMaquinaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaquinaViewHolder {
        val binding = ItemMaquinaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MaquinaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MaquinaViewHolder, position: Int) {
        val item = itens[position]
        holder.binding.tvLinha1.text = "${item.marca} / ${item.modelo}"
        holder.binding.tvLinha2.text = "Nº de série: ${item.numeroSerie}"
        holder.binding.tvLinha3.text = "Cliente: ${mapaClientes[item.clienteId] ?: "-"}"

        holder.itemView.setOnLongClickListener {
            onExcluir?.invoke(item)
            true
        }
    }

    override fun getItemCount(): Int = itens.size
}
