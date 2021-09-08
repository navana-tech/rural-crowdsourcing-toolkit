package com.microsoft.research.karya.ui.scenarios.speechData

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.microsoft.research.karya.R
import com.microsoft.research.karya.data.model.karya.enums.AssistantAudio
import com.microsoft.research.karya.databinding.NgSpeechDataMainBinding
import com.microsoft.research.karya.ui.scenarios.common.BaseMTRendererFragment
import com.microsoft.research.karya.ui.scenarios.speechData.SpeechDataMainViewModel.ButtonState.*
import com.microsoft.research.karya.utils.extensions.invisible
import com.microsoft.research.karya.utils.extensions.observe
import com.microsoft.research.karya.utils.extensions.viewBinding
import com.microsoft.research.karya.utils.extensions.viewLifecycleScope
import com.microsoft.research.karya.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NavanaSpeechDataMainFragment : BaseMTRendererFragment(R.layout.ng_speech_data_main) {
  override val TAG: String = "NAVANA_SPEECH_DATA_FRAGMENT"
  override val viewModel: SpeechDataMainViewModel by viewModels()
  val args: SpeechDataMainFragmentArgs by navArgs()
  val binding: NgSpeechDataMainBinding by viewBinding(NgSpeechDataMainBinding::bind)

  override fun requiredPermissions(): Array<String> {
    return arrayOf(android.Manifest.permission.RECORD_AUDIO)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = super.onCreateView(inflater, container, savedInstanceState)
    // TODO: Remove this once we have viewModel Factory
    viewModel.setupViewModel(args.taskId, 0, 0)
    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupObservers()

    /** Set OnBackPressed callback */
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { viewModel.onBackPressed() }

    /** record instruction */
    val recordInstruction =
      viewModel.task.params.asJsonObject.get("instruction").asString ?: getString(R.string.speech_recording_instruction)

    with(binding) {
        recordPromptTv.text = recordInstruction

        /** Set on click listeners */
        recordBtn.setOnClickListener { viewModel.handleRecordClick() }
        playBtn.setOnClickListener { viewModel.handlePlayClick() }
        nextBtn.setOnClickListener { viewModel.handleNextClick() }
        backBtn.setOnClickListener { viewModel.handleBackClick() }
    }
  }

  private fun setupObservers() {
    viewModel.backBtnState.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { state ->
      binding.backBtn.isClickable = state != DISABLED
      binding.backBtn.setBackgroundResource(
        when (state) {
            DISABLED -> R.color.colorgrey
            ENABLED -> R.color.colorGreenDarker
            ACTIVE -> R.color.colorGreenDarker
        }
      )
    }

    viewModel.recordBtnState.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { state ->
      binding.recordBtn.isClickable = state != DISABLED
      binding.recordBtn.setImageResource(
            when (state) {
                DISABLED -> R.drawable.ic_mic
                ENABLED -> R.drawable.ic_mic
                ACTIVE -> R.drawable.ic_stop
            }
        )
      binding.recordBtn.setBackgroundResource(
            when (state) {
                DISABLED -> R.color.checkUpdatesColor
                ENABLED -> R.color.checkUpdatesColor
                ACTIVE -> R.color.colorWhite
            }
        )
    }

    viewModel.playBtnState.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { state ->
      binding.playBtn.isClickable = state != DISABLED
      binding.playBtn.setImageResource(
            when (state) {
                DISABLED -> R.drawable.ic_play
                ENABLED -> R.drawable.ic_play
                ACTIVE -> R.drawable.ic_pause
            }
        )
    }

    viewModel.nextBtnState.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { state ->
      binding.nextBtn.isClickable = state != DISABLED
      binding.nextBtn.setBackgroundResource(
        when (state) {
            DISABLED -> R.color.colorgrey
            ENABLED -> R.color.colorGreenDarker
            ACTIVE -> R.color.colorGreenDarker
        }
      )
    }

    viewModel.sentenceTvText.observe(viewLifecycleOwner.lifecycle, viewLifecycleScope) { text ->
      binding.sentenceTv.text = text
    }

    viewModel.recordSecondsTvText.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { text ->
      binding.recordSecondsTv.text = text
    }

    viewModel.recordCentiSecondsTvText.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { text ->
      binding.recordCentiSecondsTv.text = text
    }

    viewModel.playbackProgressPb.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { progress ->
      binding.playbackProgressPb.progress = progress
    }

    viewModel.playbackProgressPbMax.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { max ->
      binding.playbackProgressPb.max = max
    }

    viewModel.playRecordPromptTrigger.observe(
      viewLifecycleOwner.lifecycle,
      viewLifecycleScope
    ) { play ->
      if (play) {
        playRecordPrompt()
      }
    }
  }

  private fun playRecordPrompt() {
    with(binding) {
        val oldColor = sentenceTv.currentTextColor

        assistant.playAssistantAudio(
            AssistantAudio.RECORD_SENTENCE,
            uiCue = {
                sentenceTv.setTextColor(Color.parseColor("#CC6666"))
                sentencePointerIv.visible()
            },
            onCompletionListener = {
                lifecycleScope.launch {
                    sentenceTv.setTextColor(oldColor)
                    sentencePointerIv.invisible()
                    delay(500)
                    playRecordAction()
                }
            }
        )
    }
  }

  private fun playRecordAction() {
    with(binding) {
        lifecycleScope.launch {
            assistant.playAssistantAudio(
                AssistantAudio.RECORD_ACTION,
                uiCue = {
                    recordPointerIv.visible()
                    recordBtn.setImageResource(R.drawable.ic_mic)
                    recordBtn.setBackgroundResource(R.color.purple_500)
                },
                onCompletionListener = {
                    lifecycleScope.launch {
                        recordPointerIv.invisible()
                        delay(500)
                        playStopAction()
                    }
                }
            )
            delay(1500)
            recordBtn.setImageResource(R.drawable.ic_stop)
            recordBtn.setBackgroundResource(R.color.colorWhite)
        }
    }
  }

  private fun playStopAction() {
    with(binding) {
        lifecycleScope.launch {
            assistant.playAssistantAudio(
                AssistantAudio.STOP_ACTION,
                uiCue = { recordPointerIv.visible() },
                onCompletionListener = {
                    lifecycleScope.launch {
                        recordPointerIv.invisible()
                        delay(500)
                        playListenAction()
                    }
                }
            )
            delay(500)
            recordBtn.setImageResource(R.drawable.ic_mic)
            recordBtn.setBackgroundResource(R.color.purple_500)
        }
    }
  }

  private fun playListenAction() {
    with(binding) {
        assistant.playAssistantAudio(
            AssistantAudio.LISTEN_ACTION,
            uiCue = {
                playPointerIv.visible()
                playBtn.setImageResource(R.drawable.ic_play)
            },
            onCompletionListener = {
                lifecycleScope.launch {
                    playBtn.setImageResource(R.drawable.ic_play)
                    playPointerIv.invisible()
                    delay(500)
                    playRerecordAction()
                }
            }
        )
    }
  }

  private fun playRerecordAction() {
    with(binding) {
        assistant.playAssistantAudio(
            AssistantAudio.RERECORD_ACTION,
            uiCue = {
                recordPointerIv.visible()
                recordBtn.setImageResource(R.drawable.ic_mic)
                recordBtn.setBackgroundResource(R.color.purple_500)
            },
            onCompletionListener = {
                lifecycleScope.launch {
                    recordBtn.setImageResource(R.drawable.ic_mic)
                    recordBtn.setBackgroundResource(R.color.purple_500)
                    recordPointerIv.invisible()
                    delay(500)
                    playNextAction()
                }
            }
        )
    }
  }

  private fun playNextAction() {
    with(binding) {
        assistant.playAssistantAudio(
            AssistantAudio.NEXT_ACTION,
            uiCue = {
                nextPointerIv.visible()
                nextBtn.setImageResource(R.drawable.ic_next)
            },
            onCompletionListener = {
                lifecycleScope.launch {
                    nextBtn.setImageResource(R.drawable.ic_next)
                    nextPointerIv.invisible()
                    delay(500)
                    playPreviousAction()
                }
            }
        )
    }
  }

  private fun playPreviousAction() {
    with(binding) {
        assistant.playAssistantAudio(
            AssistantAudio.PREVIOUS_ACTION,
            uiCue = {
                backPointerIv.visible()
                backBtn.setImageResource(R.drawable.ic_prev)
            },
            onCompletionListener = {
                lifecycleScope.launch {
                    backBtn.setImageResource(R.drawable.ic_prev)
                    backPointerIv.invisible()
                    delay(500)
                    viewModel.moveToPrerecording()
                }
            }
        )
    }
  }

  override fun onStop() {
    super.onStop()
    viewModel.cleanupOnStop()
  }
}
