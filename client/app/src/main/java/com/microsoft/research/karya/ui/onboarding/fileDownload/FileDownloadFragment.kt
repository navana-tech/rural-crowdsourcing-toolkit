package com.microsoft.research.karya.ui.onboarding.fileDownload

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.manager.AuthManager
import com.microsoft.research.karya.data.manager.ResourceManager
import com.microsoft.research.karya.databinding.FragmentFileDownloadBinding
import com.microsoft.research.karya.ui.onboarding.accesscode.AccessCodeViewModel
import com.microsoft.research.karya.utils.Result
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycle
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import com.zabaan.sdk.Zabaan
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FileDownloadFragment : Fragment(R.layout.fragment_file_download) {

  private val viewModel by viewModels<AccessCodeViewModel>()
  @Inject lateinit var resourceManager: ResourceManager
  @Inject lateinit var authManager: AuthManager
  private val binding by viewBinding(FragmentFileDownloadBinding::bind)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    downloadResourceFiles()
  }

   override fun onResume() {
       super.onResume()
       Zabaan.getInstance().show(binding.root, viewLifecycle)
       Zabaan.getInstance().setScreenName("FILE_DOWNLOAD")
       Zabaan.getInstance().setCurrentState("IDLE")
    }

   override fun onPause() {
     Zabaan.getInstance().stopZabaanInteraction()
     super.onPause()
   }

    private fun downloadResourceFiles() {
    viewLifecycleScope.launch {
      val worker = authManager.fetchLoggedInWorker()

      val fileDownloadFlow = resourceManager.downloadLanguageResources(worker.accessCode, worker.language)

      fileDownloadFlow.observe(viewLifecycle, viewLifecycleScope) { result ->
        when (result) {
          is Result.Success<*> -> navigateToRegistration()
          is Result.Error -> {
            Toast.makeText(requireContext(), "Could not download resources", Toast.LENGTH_LONG).show()
            navigateToRegistration()
          }
          Result.Loading -> {}
        }
      }
    }
  }

  private fun navigateToRegistration() {
    findNavController().navigate(R.id.action_fileDownloadFragment2_to_consentFormFragment22)
  }
}
