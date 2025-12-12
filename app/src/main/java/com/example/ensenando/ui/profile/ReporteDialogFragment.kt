package com.example.ensenando.ui.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ensenando.databinding.DialogReporteBinding
import java.io.File

class ReporteDialogFragment : DialogFragment() {
    private var _binding: DialogReporteBinding? = null
    private val binding get() = _binding!!
    private var pdfPath: String? = null
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    companion object {
        private const val ARG_PDF_PATH = "pdf_path"

        fun newInstance(pdfPath: String): ReporteDialogFragment {
            val fragment = ReporteDialogFragment()
            val args = Bundle()
            args.putString(ARG_PDF_PATH, pdfPath)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.example.ensenando.R.style.FullScreenDialogStyle)
        pdfPath = arguments?.getString(ARG_PDF_PATH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReporteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPdfViewer()
        setupButtons()
    }

    private fun setupPdfViewer() {
        pdfPath?.let { path ->
            val file = File(path)
            if (!file.exists()) {
                binding.tvReportePath.text = "⚠️ Error: No se pudo encontrar el archivo del reporte"
                return
            }

            try {
                // Abrir el archivo PDF
                fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                pdfRenderer = PdfRenderer(fileDescriptor!!)

                // Mostrar información del PDF
                val pageCount = pdfRenderer!!.pageCount
                binding.tvReportePath.text = """
                    📄 Reporte Generado Exitosamente
                    📁 Archivo: ${file.name}
                    📊 Tamaño: ${file.length() / 1024} KB
                    📑 Páginas: $pageCount
                    Desliza para ver todas las páginas del reporte.
                """.trimIndent()

                // Configurar RecyclerView para mostrar páginas del PDF
                setupPdfPagesRecyclerView()

            } catch (e: Exception) {
                android.util.Log.e("ReporteDialog", "Error al abrir PDF", e)
                binding.tvReportePath.text = "⚠️ Error al abrir el PDF: ${e.message}"
            }
        }
    }

    private fun setupPdfPagesRecyclerView() {
        pdfRenderer?.let { renderer ->
            val adapter = PdfPageAdapter(renderer) { bitmap ->
                // Mostrar la página en un ImageView si es necesario
                // Por ahora, el adapter maneja la visualización
            }
            // Usar orientación vertical para que las páginas se muestren una debajo de otra
            binding.rvPdfPages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            binding.rvPdfPages.adapter = adapter
            binding.rvPdfPages.visibility = View.VISIBLE
        }
    }

    private fun setupButtons() {
        binding.btnDescargarPdf.setOnClickListener {
            pdfPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    // Descargar el PDF usando ACTION_VIEW para abrir con aplicación predeterminada
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        // Si no hay aplicación para abrir, usar compartir como alternativa
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Descargar o compartir PDF"))
                    }
                }
            }
        }

        binding.btnCompartir.setOnClickListener {
            pdfPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "Compartir reporte"))
                }
            }
        }

        binding.btnCerrar.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pdfRenderer?.close()
        fileDescriptor?.close()
        pdfRenderer = null
        fileDescriptor = null
        _binding = null
    }
}