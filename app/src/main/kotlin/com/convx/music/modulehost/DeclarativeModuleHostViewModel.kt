package com.convx.music.modulehost

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.convx.modulehost.DeclarativeModuleHostSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DeclarativeModuleHostViewModel @Inject constructor(
    private val moduleHost: ConvxDeclarativeModuleHost,
) : ViewModel() {
    private val _snapshot = MutableStateFlow(moduleHost.snapshot())
    val snapshot: StateFlow<DeclarativeModuleHostSnapshot> = _snapshot.asStateFlow()

    /** User-facing failure surfaced by the screen (e.g. picker read, validation). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun installPackage(resolver: ContentResolver, uri: Uri) {
        mutate {
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("cannot read the selected file")
            }
            moduleHost.install(bytes)
        }
    }

    fun enable(moduleId: String) {
        mutate { moduleHost.enable(moduleId) }
    }

    fun disable(moduleId: String) {
        mutate { moduleHost.disable(moduleId) }
    }

    fun remove(moduleId: String) {
        mutate { moduleHost.remove(moduleId) }
    }

    fun dismissError() {
        _error.value = null
    }

    private fun mutate(operation: suspend () -> DeclarativeModuleHostSnapshot) {
        viewModelScope.launch {
            _busy.value = true
            try {
                _snapshot.value = operation()
            } catch (error: Exception) {
                _error.value = error.message ?: error.javaClass.simpleName
            } finally {
                _busy.value = false
            }
        }
    }
}
