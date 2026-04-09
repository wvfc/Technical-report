package com.soutech.relatoriotecnico.ui.maquina

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.soutech.relatoriotecnico.data.AppDatabase
import com.soutech.relatoriotecnico.data.ClienteEntity
import com.soutech.relatoriotecnico.data.MaquinaEntity
import com.soutech.relatoriotecnico.databinding.ActivityCadastroMaquinaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CadastroMaquinaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroMaquinaBinding
    private var clientes: List<ClienteEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroMaquinaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Cadastro de Máquina"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        carregarClientes()

        binding.btnSalvarMaquina.setOnClickListener {
            salvarMaquina()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun carregarClientes() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            clientes = withContext(Dispatchers.IO) {
                db.clienteDao().listarTodos()
            }

            if (clientes.isEmpty()) {
                Toast.makeText(
                    this@CadastroMaquinaActivity,
                    "Cadastre um cliente antes de cadastrar máquinas.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val nomes = clientes.map { it.nomeFantasia }
                val adapter = ArrayAdapter(
                    this@CadastroMaquinaActivity,
                    android.R.layout.simple_spinner_item,
                    nomes
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerCliente.adapter = adapter
            }
        }
    }

    private fun salvarMaquina() {
        if (clientes.isEmpty()) {
            Toast.makeText(this, "Cadastre pelo menos um cliente primeiro.", Toast.LENGTH_SHORT).show()
            return
        }

        val idxCliente = binding.spinnerCliente.selectedItemPosition
        if (idxCliente < 0 || idxCliente >= clientes.size) {
            Toast.makeText(this, "Selecione um cliente.", Toast.LENGTH_SHORT).show()
            return
        }
        val cliente = clientes[idxCliente]

        val marca = binding.etMarca.text.toString().trim()
        val modelo = binding.etModelo.text.toString().trim()
        val modeloIhm = binding.etModeloIhm.text.toString().trim()
        val numeroSerie = binding.etNumeroSerie.text.toString().trim()
        val fotoPlaqueta = binding.etFotoPlaqueta.text.toString().trim()
        val fotoCompressor = binding.etFotoCompressor.text.toString().trim()

        if (marca.isEmpty() || modelo.isEmpty() || numeroSerie.isEmpty()) {
            Toast.makeText(
                this,
                "Informe pelo menos marca, modelo e número de série.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnSalvarMaquina.isEnabled = false

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@CadastroMaquinaActivity)
                db.maquinaDao().inserir(
                    MaquinaEntity(
                        clienteId = cliente.id,
                        marca = marca,
                        modelo = modelo,
                        modeloIhm = modeloIhm.ifEmpty { null },
                        numeroSerie = numeroSerie,
                        fotoPlaquetaUri = fotoPlaqueta.ifEmpty { null },
                        fotoCompressorUri = fotoCompressor.ifEmpty { null }
                    )
                )
            }

            Toast.makeText(
                this@CadastroMaquinaActivity,
                "Máquina salva com sucesso.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }
}
