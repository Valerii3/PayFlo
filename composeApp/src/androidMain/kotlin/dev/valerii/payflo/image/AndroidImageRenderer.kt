package dev.valerii.payflo.image

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun ByteArrayImage(
    imageBytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}