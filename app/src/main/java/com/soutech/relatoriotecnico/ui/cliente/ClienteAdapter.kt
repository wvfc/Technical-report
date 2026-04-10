package com.soutech.relatoriotecnico.ui.cliente

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.databinding.ItemClienteBinding

class ClienteAdapter(
    private var clientes: List<ClienteEntity>,
    private val onClick: (ClienteEntity) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.Holder>() {

    inner class Holder(val binding: ItemClienteBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val b = ItemClienteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(b)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val c = clientes[position]
        holder.binding.tvNomeCliente.text = c.nomeFantasia
        holder.binding.tvDocumentoCliente.text = when {
            !c.documento.isNullOrBlank() -> c.documento
            c.razaoSocial.isNotBlank() -> c.razaoSocial
            else -> "Sem documento"
        }
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun getItemCount(): Int = clientes.size

    fun atualizar(nova: List<ClienteEntity>) {
        clientes = nova
        notifyDataSetChanged()
    }
}
