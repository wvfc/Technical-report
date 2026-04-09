package com.soutech.relatoriotecnico.ui.tecnico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.data.TecnicoEntity
import com.soutech.relatoriotecnico.databinding.ItemTecnicoBinding

class TecnicoAdapter(
    private val itens: List<TecnicoEntity>,
    private val onExcluir: ((TecnicoEntity) -> Unit)? = null
) : RecyclerView.Adapter<TecnicoAdapter.TecnicoViewHolder>() {

    inner class TecnicoViewHolder(val binding: ItemTecnicoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TecnicoViewHolder {
        val binding = ItemTecnicoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TecnicoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TecnicoViewHolder, position: Int) {
        val item = itens[position]
        holder.binding.tvNomeTecnico.text = item.nome
        holder.binding.tvFuncaoTecnico.text = item.funcao ?: "-"

        holder.itemView.setOnLongClickListener {
            onExcluir?.invoke(item)
            true
        }
    }

    override fun getItemCount(): Int = itens.size
}
