package dev.valerii.payflo.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ByteArrayImage(
    imageBytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val nsData = imageBytes.toNSData()
    val uiImage = UIImage.imageWithData(nsData)

    if (uiImage != null) {
        UIKitView(
            modifier = modifier,
            factory = {
                UIImageView(image = uiImage).apply {
                    setContentMode(UIViewContentMode.UIViewContentModeScaleAspectFill)
                }
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
}
