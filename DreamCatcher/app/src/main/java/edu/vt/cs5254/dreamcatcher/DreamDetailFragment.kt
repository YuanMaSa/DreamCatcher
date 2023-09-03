package edu.vt.cs5254.dreamcatcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import kotlinx.coroutines.launch
import java.io.File

class DreamDetailFragment : Fragment() {

    private val viewModel: DreamDetailViewModel by viewModels {
        DreamDetailViewModelFactory(args.dreamId)
    }
    private val args: DreamDetailFragmentArgs by navArgs()
    private var bindingState: FragmentDreamDetailBinding? = null
    private val binding
        get() = checkNotNull(bindingState) {
            "FragmentDreamDetail binding is null"
        }
    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) {
        binding.dreamPhoto.tag = null
        viewModel.dream.value?.let { updatePhoto(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingState = FragmentDreamDetailBinding.inflate(inflater, container, false)

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_dream_detail, menu)

                    val captureImageIntent = takePhoto.contract.createIntent(
                        requireContext(),
                        Uri.EMPTY // NOTE: The "null" used in BNRG is obsolete now
                    )
                    menu.findItem(R.id.take_photo_menu).isVisible =
                        canResolveIntent(captureImageIntent)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.share_dream_menu -> {
                            val reportIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    viewModel.dream.value?.let { createDreamShare(it) })
                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    getString(R.string.dream_report_subject)
                                )
                            }
                            val chooserIntent = Intent.createChooser(
                                reportIntent,
                                getString(R.string.send_report)
                            )
                            startActivity(chooserIntent)
                            true
                        }
                        R.id.take_photo_menu -> {
                            viewModel.dream.value?.let {
                                val photoFile = File(
                                    requireContext().applicationContext.filesDir,
                                    it.photoFileName
                                )
                                val photoUri = FileProvider.getUriForFile(
                                    requireContext(),
                                    "edu.vt.cs5254.dreamcatcher.fileprovider",
                                    photoFile
                                )
                                takePhoto.launch(photoUri)
                            }
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner
        )

        binding.dreamEntryRecycler.layoutManager = LinearLayoutManager(context)
        getItemTouchHelper().attachToRecyclerView(binding.dreamEntryRecycler)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dream.collect { dream ->
                    dream?.let { updateView(dream) }
                }
            }
        }
        binding.titleText.doOnTextChanged { text, _, _, _ ->
            viewModel.updateDream { prevDream ->
                prevDream.copy(title = text.toString())
                    .apply { entries = prevDream.entries }
            }
        }

        binding.deferredCheckbox.setOnClickListener {
            viewModel.updateDream { prevDream ->
                if (prevDream.isDeferred) {
                    prevDream.copy().apply {
                        entries = prevDream.entries.filter { it.kind != DreamEntryKind.DEFERRED }
                    }
                } else {
                    prevDream.copy().apply {
                        entries = prevDream.entries + DreamEntry(
                            kind = DreamEntryKind.DEFERRED,
                            dreamId = prevDream.id
                        )
                    }
                }
            }
        }

        binding.fulfilledCheckbox.setOnClickListener {
            viewModel.updateDream { prevDream ->
                if (prevDream.isFulfilled) {
                    prevDream.copy().apply {
                        entries = prevDream.entries.filter { it.kind != DreamEntryKind.FULFILLED }
                    }
                } else {
                    prevDream.copy().apply {
                        entries = prevDream.entries + DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            dreamId = prevDream.id
                        )
                    }
                }
            }
        }
        setFragmentResultListener(
            ReflectionDialogFragment.REQUEST_KEY
        ) { _, bundle ->
            val entryText = bundle.getString(ReflectionDialogFragment.BUNDLE_KEY) ?: ""
            viewModel.updateDream { prevDream ->
                val newReflection = DreamEntry(
                    text = entryText,
                    kind = DreamEntryKind.REFLECTION,
                    dreamId = prevDream.id
                )
                prevDream.copy().apply {
                    entries = prevDream.entries + newReflection
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingState = null
    }

    private fun updateView(dream: Dream) {
        binding.dreamEntryRecycler.adapter = DreamEntryAdapter(dream.entries)
        displayReflectionButton(dream)

        val dateString = DateFormat.format("yyyy-MM-dd 'at' hh:mm:ss a", dream.lastUpdated)
        binding.lastUpdatedText.text = "Last updated $dateString"
        if (dream.title != binding.titleText.text.toString()) {
            binding.titleText.setText(dream.title)
        }
        binding.deferredCheckbox.isChecked = dream.isDeferred
        binding.fulfilledCheckbox.isChecked = dream.isFulfilled
        binding.deferredCheckbox.isEnabled = !dream.isFulfilled
        binding.fulfilledCheckbox.isEnabled = !dream.isDeferred

        updatePhoto(dream)
        binding.dreamPhoto.isEnabled = binding.dreamPhoto.tag != null
        binding.dreamPhoto.setOnClickListener {
            findNavController().navigate(
                DreamDetailFragmentDirections.showPhotoDetail(dream.photoFileName)
            )
        }
    }

    private fun updatePhoto(dream: Dream) {
        with(binding.dreamPhoto) {
            if (tag != dream.photoFileName) {
                val photoFile =
                    File(requireContext().applicationContext.filesDir, dream.photoFileName)
                if (photoFile.exists()) {
                    doOnLayout { measuredView ->
                        val scaledBM = getScaledBitmap(
                            photoFile.path,
                            measuredView.width,
                            measuredView.height
                        )
                        setImageBitmap(scaledBM)
                        tag = dream.photoFileName
                        binding.dreamPhoto.isEnabled = true
                    }
                } else {
                    setImageBitmap(null)
                    tag = null
                    isEnabled = false
                }
            }
        }
    }


    private fun displayReflectionButton(dream: Dream) {
        if (dream.isFulfilled) {
            binding.addReflectionButton?.hide()
        } else {
            binding.addReflectionButton?.show()
        }
        binding.addReflectionButton?.isEnabled = !dream.isFulfilled
        binding.addReflectionButton?.setOnClickListener {
            findNavController().navigate(
                DreamDetailFragmentDirections.addReflection()
            )
        }
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null
    }

    private fun createDreamShare(dream: Dream): String {
        val dateString = DateFormat.format("yyyy-MM-dd 'at' hh:mm:ss a", dream.lastUpdated)
        val lastUpdated = getString(R.string.last_updated, dateString)

        val reflectionString = when {
            dream.entries.any { it.kind == DreamEntryKind.REFLECTION } ->
                "Reflections:\n" + dream.entries.filter { it.kind == DreamEntryKind.REFLECTION }
                    .joinToString(separator = "\n") { " * " + it.text }
            else -> ""
        }

        val status = when {
            dream.isFulfilled -> getString(R.string.dream_share_status, "Fulfilled.")
            dream.isDeferred -> getString(R.string.dream_share_status, "Deferred.")
            else -> ""
        }
        return getString(R.string.dream_share, dream.title, lastUpdated, reflectionString, status)
    }

    private fun getItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val dreamEntryHolder = viewHolder as DreamEntryHolder
                    val dreamEntry = dreamEntryHolder.boundEntry
                    viewModel.updateDream { dream ->
                        dream.copy().apply {
                            entries = dream.entries.filter { it != dreamEntry }
                        }
                    }
                }
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dreamEntryHolder = viewHolder as DreamEntryHolder
                val dreamEntry = dreamEntryHolder.boundEntry
                return if (dreamEntry.kind == DreamEntryKind.REFLECTION) ItemTouchHelper.LEFT else 0
            }
        })
    }
}
