package com.soutech.relatoriotecnico.ui.cliente

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.databinding.ActivityClienteFormBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClienteFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClienteFormBinding

    // Preenchido ao editar um cliente existente
    private var clienteEditando: ClienteEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClienteFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Cliente"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val clienteId = intent.getLongExtra("clienteId", -1L).takeIf { it > 0 }

        if (clienteId != null) {
            carregarCliente(clienteId)
        }

        binding.btnSalvarCliente.setOnClickListener {
            salvarCliente()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarCliente(clienteId: Long) {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val cliente = withContext(Dispatchers.IO) {
                db.clienteDao().buscarPorId(clienteId)
            }
            cliente?.let {
                clienteEditando = it
                binding.edRazaoSocial.setText(it.razaoSocial)
                binding.etNomeCliente.setText(it.nomeFantasia)
                binding.etCnpj.setText(it.documento ?: "")
                binding.etEndereco.setText(it.endereco ?: "")
                binding.edEmail.setText(it.email ?: "")
                binding.edTelefone.setText(it.telefone ?: "")
                binding.edWhatsapp.setText(it.whatsapp ?: "")
                supportActionBar?.title = "Editar Cliente"
            }
        }
    }

    private fun salvarCliente() {
        val razaoSocial = binding.edRazaoSocial.text.toString().trim()
        val nomeFantasia = binding.etNomeCliente.text.toString().trim()
        val cnpj = binding.etCnpj.text.toString().trim()
        val endereco = binding.etEndereco.text.toString().trim()
        val email = binding.edEmail.text.toString().trim()
        val telefone = binding.edTelefone.text.toString().trim()
        val whatsapp = binding.edWhatsapp.text.toString().trim()

        if (nomeFantasia.isEmpty()) {
            Toast.makeText(this, "Informe o nome fantasia do cliente.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSalvarCliente.isEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@ClienteFormActivity)
                val editando = clienteEditando

                if (editando != null) {
                    // Atualiza cliente existente
                    db.clienteDao().atualizar(
                        editando.copy(
                            razaoSocial = razaoSocial,
                            nomeFantasia = nomeFantasia,
                            documento = cnpj.ifEmpty { null },
                            endereco = endereco.ifEmpty { null },
                            email = email.ifEmpty { null },
                            telefone = telefone.ifEmpty { null },
                            whatsapp = whatsapp.ifEmpty { null }
                        )
                    )
                } else {
                    // Insere novo cliente
                    db.clienteDao().inserir(
                        ClienteEntity(
                            razaoSocial = razaoSocial,
                            nomeFantasia = nomeFantasia,
                            documento = cnpj.ifEmpty { null },
                            endereco = endereco.ifEmpty { null },
                            email = email.ifEmpty { null },
                            telefone = telefone.ifEmpty { null },
                            whatsapp = whatsapp.ifEmpty { null }
                        )
                    )
                }
            }

            Toast.makeText(
                this@ClienteFormActivity,
                "Cliente salvo com sucesso.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}
