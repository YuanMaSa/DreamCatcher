package edu.vt.cs5254.dreamcatcher

import androidx.lifecycle.ViewModel

class DreamListViewModel : ViewModel() {

    private val dreamRepository = DreamRepository.get()
    val dreams = dreamRepository.getDreams()

    suspend fun addDream(dream: Dream) {
        dreamRepository.addDream(dream)
    }

    suspend fun deleteDream(dream: Dream) {
        dreamRepository.deleteDream(dream)
    }
}