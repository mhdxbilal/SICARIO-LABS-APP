package com.example.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.viewmodel.DownloadUiState
import com.example.viewmodel.DownloadViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaylistsFragment : Fragment() {

    private lateinit var viewModel: DownloadViewModel
    private var selectedFolderUri: Uri? = null

    // UI elements references
    private lateinit var urlEditText: EditText
    private lateinit var qualitySpinner: Spinner
    private lateinit var selectFolderButton: Button
    private lateinit var downloadButton: Button
    private lateinit var cancelButton: Button
    private lateinit var folderStatusText: TextView
    private lateinit var statusTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var downloadProgressBar: ProgressBar

    /**
     * 1. ActivityResultLauncher for the directory OpenDocumentTree.
     * Requests directories, resolves permission persistability, and saves URI in shared preferences.
     */
    private val folderSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Secure modern long-term persistable storage permission access
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                selectedFolderUri = uri
                folderStatusText.text = "Location: ${uri.lastPathSegment ?: uri.path}"
                
                // Persist Uri selection in SharedPreferences
                val prefs = requireContext().getSharedPreferences("downloader_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("preferred_folder_uri", uri.toString()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Permission Granting Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[DownloadViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Safe programmatic layout creation for compiling out-of-the-box inside any project
        val context = requireContext()
        val root = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.parseColor("#0F0F12"))
        }

        urlEditText = EditText(context).apply {
            hint = "Enter/Paste Media stream stream URL"
            setHintTextColor(android.graphics.Color.GRAY)
            setTextColor(android.graphics.Color.WHITE)
            id = View.generateViewId()
        }
        root.addView(urlEditText)

        qualitySpinner = Spinner(context).apply {
            id = View.generateViewId()
        }
        root.addView(qualitySpinner)

        selectFolderButton = Button(context).apply {
            text = "Select Destination Folder"
            id = View.generateViewId()
        }
        root.addView(selectFolderButton)

        folderStatusText = TextView(context).apply {
            text = "No custom folder selected."
            setTextColor(android.graphics.Color.LTGRAY)
            id = View.generateViewId()
        }
        root.addView(folderStatusText)

        downloadButton = Button(context).apply {
            text = "EXECUTE DOWNLOAD"
            id = View.generateViewId()
        }
        root.addView(downloadButton)

        cancelButton = Button(context).apply {
            text = "CANCEL"
            visibility = View.GONE
            id = View.generateViewId()
        }
        root.addView(cancelButton)

        statusTextView = TextView(context).apply {
            text = "Status: Idle"
            setTextColor(android.graphics.Color.WHITE)
            id = View.generateViewId()
        }
        root.addView(statusTextView)

        speedTextView = TextView(context).apply {
            text = "Speed: 0 B/s"
            setTextColor(android.graphics.Color.CYAN)
            id = View.generateViewId()
        }
        root.addView(speedTextView)

        downloadProgressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            id = View.generateViewId()
        }
        root.addView(downloadProgressBar)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore previously persisted URI choice if available
        val prefs = requireContext().getSharedPreferences("downloader_prefs", Context.MODE_PRIVATE)
        val savedFolderString = prefs.getString("preferred_folder_uri", "") ?: ""
        if (savedFolderString.isNotEmpty()) {
            try {
                selectedFolderUri = Uri.parse(savedFolderString)
                folderStatusText.text = "Location: ${selectedFolderUri?.lastPathSegment ?: selectedFolderUri?.path}"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Folder selection click controller
        selectFolderButton.setOnClickListener {
            folderSelectionLauncher.launch(null)
        }

        // 2. Click Handler - Extract variables & Initiate execution pipeline
        downloadButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(requireContext(), "Please paste a video URL first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Extract the selected quality parameter (Default to "best")
            val quality = qualitySpinner.selectedItem?.toString() ?: "best"
            
            // Initiate the background thread-safe engine
            viewModel.initiateDownload(url, quality, selectedFolderUri)
        }

        cancelButton.setOnClickListener {
            viewModel.cancelDownload()
        }

        // 3. Thread-safe StateFlow observation on Main thread
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    mapUiState(state)
                }
            }
        }
    }

    /**
     * Maps the state changes cleanly to the UI variables on the main thread safely.
     */
    private fun mapUiState(state: DownloadUiState) {
        when (state) {
            is DownloadUiState.Idle -> {
                statusTextView.text = "Engine Standby. Ready for connections."
                speedTextView.text = ""
                downloadProgressBar.progress = 0
                downloadButton.isEnabled = true
                cancelButton.visibility = View.GONE
            }
            is DownloadUiState.Loading -> {
                statusTextView.text = "Extracting media parameters..."
                speedTextView.text = ""
                downloadProgressBar.progress = 0
                downloadButton.isEnabled = false
                cancelButton.visibility = View.VISIBLE
            }
            is DownloadUiState.Downloading -> {
                statusTextView.text = "Downloading: ${state.progress}%"
                speedTextView.text = "Current speed: ${state.speed}"
                downloadProgressBar.progress = state.progress
                downloadButton.isEnabled = false
                cancelButton.visibility = View.VISIBLE
            }
            is DownloadUiState.Completed -> {
                statusTextView.text = "Download complete successfully!"
                speedTextView.text = "Location: ${state.filePath}"
                downloadProgressBar.progress = 100
                downloadButton.isEnabled = true
                cancelButton.visibility = View.GONE
                Toast.makeText(requireContext(), "File saved to ${state.filePath}", Toast.LENGTH_LONG).show()
            }
            is DownloadUiState.Error -> {
                statusTextView.text = "Error: ${state.message}"
                speedTextView.text = ""
                downloadProgressBar.progress = 0
                downloadButton.isEnabled = true
                cancelButton.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed: ${state.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
