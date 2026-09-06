package com.convx.music.modulehost

import androidx.lifecycle.ViewModel
import com.convx.modulehost.DeclarativeModuleHostSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DeclarativeModuleHostViewModel @Inject constructor(
    moduleHost: ConvxDeclarativeModuleHost,
) : ViewModel() {
    val snapshot: DeclarativeModuleHostSnapshot = moduleHost.snapshot()
}
