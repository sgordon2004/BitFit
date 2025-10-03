package com.example.bitfit.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HealthEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).healthEntryDao()

    val entries: Flow<List<HealthEntry>> = dao.getAll()

    fun addEntry(metric: String, dateIso: String, value: Int, notes: String?) = viewModelScope.launch {
        dao.insert(
            HealthEntry(
                date = dateIso, // "YYYY-MM-DD"
                metric = metric, // "diet" | "water" | "exercise" | "mood"
                value = value,
                notes = notes
            )
        )
    }

    fun delete(entry: HealthEntry) = viewModelScope.launch {
        dao.delete(entry)
    }

    fun updateEntry(entry: HealthEntry) = viewModelScope.launch {
        dao.update(entry)
    }
}