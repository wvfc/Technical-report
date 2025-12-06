package com.soutech.relatoriotecnico.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soutech.relatoriotecnico.databinding.ActivityMainBinding
import com.soutech.relatoriotecnico.ui.cliente.ClienteFormActivity
import com.soutech.relatoriotecnico.ui.cliente.ClienteListaActivity
import com.soutech.relatoriotecnico.ui.maquina.CadastroMaquinaActivity
import com.soutech.relatoriotecnico.ui.maquina.MaquinasCadastradasActivity
import com.soutech.relatoriotecnico.ui.relatorio.RelatorioListaActivity
import com.soutech.relatoriotecnico.ui.relatorio.TipoRelatorioActivity
import com.soutech.relatoriotecnico.ui.tecnico.CadastroTecnicoActivity
import com.soutech.relatoriotecnico.ui.tecnico.TecnicosCadastradosActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Oculta a ActionBar padrão
        supportActionBar?.hide()

        // =============== AJUDA TÉCNICO (abre PDFs) ===============
        binding.btnSenhasIHMs.setOnClickListener {
            abrirPdf("https://login.soutechautomacao.com/static/pdfs/senhas_ihm.pdf")
        }

        binding.btnPdfsMaquinas.setOnClickListener {
            abrirPdf("https://login.soutechautomacao.com/static/pdfs/pdfs_maquinas.pdf")
        }

        binding.btnDuvidasComuns.setOnClickListener {
            abrirPdf("https://login.soutechautomacao.com/static/pdfs/duvidas_comuns.pdf")
        }

        // =============== NOVO RELATÓRIO ===============
        binding.btnNovoRelatorio.setOnClickListener {
            startActivity(Intent(this, TipoRelatorioActivity::class.java))
        }

        // =============== HISTÓRICO DE RELATÓRIOS ===============
        binding.btnHistoricoRelatorios.setOnClickListener {
            startActivity(Intent(this, RelatorioListaActivity::class.java))
        }

        // =============== CADASTROS ===============
        binding.btnCadastroCliente.setOnClickListener {
            startActivity(Intent(this, ClienteFormActivity::class.java))
        }

        binding.btnClientesCadastrados.setOnClickListener {
            startActivity(Intent(this, ClienteListaActivity::class.java))
        }

        // Máquinas – agora abrindo as telas novas
        binding.btnCadastroMaquina.setOnClickListener {
            startActivity(Intent(this, CadastroMaquinaActivity::class.java))
        }

        binding.btnMaquinasCadastradas.setOnClickListener {
            startActivity(Intent(this, MaquinasCadastradasActivity::class.java))
        }

        // Técnicos – ainda em desenvolvimento
        binding.btnCadastroTecnico.setOnClickListener {
            startActivity(Intent(this, CadastroTecnicoActivity::class.java))
        } 

        binding.btnTecnicosCadastrados.setOnClickListener {
            startActivity(Intent(this, TecnicosCadastradosActivity::class.java))
        }

    }

    private fun abrirPdf(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Nenhum aplicativo de PDF encontrado no dispositivo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
