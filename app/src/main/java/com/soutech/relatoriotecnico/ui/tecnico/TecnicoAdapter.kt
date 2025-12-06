package com.soutech.relatoriotecnico.ui.tecnico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.databinding.ItemTecnicoBinding

data class TecnicoDto(
    val id: Int,
    val name: String,
    val role: String?
)

class TecnicoAdapter(
    private val itens: List<TecnicoDto>
) : RecyclerView.Adapter<TecnicoAdapter.TecnicoViewHolder>() {

    inner class TecnicoViewHolder(val binding: ItemTecnicoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TecnicoViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTecnicoBinding.inflate(inflater, parent, false)
        return TecnicoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TecnicoViewHolder, position: Int) {
        val item = itens[position]
        holder.binding.tvNomeTecnico.text = item.name
        holder.binding.tvFuncaoTecnico.text = item.role ?: "-"
    }

    override fun getItemCount(): Int = itens.size
}
