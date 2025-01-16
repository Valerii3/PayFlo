package dev.valerii.payflo.image

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit
