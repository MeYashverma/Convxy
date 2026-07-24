package com.music.spine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModuleIndex(
    @SerialName("category:modules") val modules: List<SpineModule> = emptyList(),
    @SerialName("category:artworks") val artworks: List<SpineModule> = emptyList(),
    @SerialName("category:music") val music: List<SpineModule> = emptyList(),
    @SerialName("category:debrid_modules") val debrid: List<SpineModule> = emptyList(),
    @SerialName("category:testing") val testing: List<SpineModule> = emptyList(),
) {
    val allModules: List<SpineModule>
        get() = modules + music + debrid
}
