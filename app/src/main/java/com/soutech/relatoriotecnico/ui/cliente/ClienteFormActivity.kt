package com.soutech.relatoriotecnico.ui.cliente

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.databinding.ActivityClienteFormBinding
import kotlinx.coroutines.launch

class ClienteFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteFormBinding
    private var clienteId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de cliente"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        clienteId = intent.getLongExtra("clienteId", -1L).takeIf { it > 0 }

        if (clienteId != null) {
            carregarCliente(clienteId!!)
        }

        binding.btnSalvarCliente.setOnClickListener {
            salvarCliente()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarCliente(id: Long) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val cliente = db.clienteDao().buscarPorId(id)
            cliente?.let {
                binding.edRazaoSocial.setText(it.razaoSocial)
                binding.edNomeFantasia.setText(it.nomeFantasia)
                binding.edDocumento.setText(it.documento ?: "")
                binding.edEndereco.setText(it.endereco ?: "")
                binding.edEmail.setText(it.email ?: "")
                binding.edTelefone.setText(it.telefone ?: "")
                binding.edWhatsapp.setText(it.whatsapp ?: "")
            }
        }
    }

    private fun salvarCliente() {
        val razao = binding.edRazaoSocial.text.toString().trim()
        val fantasia = binding.edNomeFantasia.text.toString().trim()
        val documento = binding.edDocumento.text.toString().trim().ifEmpty { null }
        val endereco = binding.edEndereco.text.toString().trim().ifEmpty { null }
        val email = binding.edEmail.text.toString().trim().ifEmpty { null }
        val telefone = binding.edTelefone.text.toString().trim().ifEmpty { null }
        val whatsapp = binding.edWhatsapp.text.toString().trim().ifEmpty { null }

        if (razao.isEmpty() || fantasia.isEmpty()) {
            Toast.makeText(this, "Razão Social e Nome Fantasia são obrigatórios.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            if (clienteId == null) {
                val novo = ClienteEntity(
                    razaoSocial = razao,
                    nomeFantasia = fantasia,
                    documento = documento,
                    endereco = endereco,
                    email = email,
                    telefone = telefone,
                    whatsapp = whatsapp
                )
                db.clienteDao().inserir(novo)
                Toast.makeText(this@ClienteFormActivity, "Cliente cadastrado.", Toast.LENGTH_SHORT).show()
            } else {
                val existente = ClienteEntity(
                    id = clienteId!!,
                    razaoSocial = razao,
                    nomeFantasia = fantasia,
                    documento = documento,
                    endereco = endereco,
                    email = email,
                    telefone = telefone,
                    whatsapp = whatsapp
                )
                db.clienteDao().atualizar(existente)
                Toast.makeText(this@ClienteFormActivity, "Cliente atualizado.", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
