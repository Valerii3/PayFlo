package dev.valerii.payflo

import androidx.compose.ui.interop.UIKitView
import platform.UIKit.*
import platform.Foundation.NSData
import platform.CoreGraphics.CGSizeMake
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.dataWithBytes
import platform.darwin.NSObject
import kotlinx.cinterop.*


fun MainViewController() = ComposeUIViewController { App() }


@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val delegate = remember { IosImagePickerDelegate() }

    return {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        val picker = delegate.createPicker(onImagePicked)
        rootController?.presentViewController(picker, true, null)
    }
}

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


// iosMain
class IosImagePickerDelegate : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    private var onImagePicked: ((ByteArray) -> Unit)? = null

    // Keep a strong reference to the picker to prevent deallocation
    private var pickerController: UIImagePickerController? = null

    fun createPicker(callback: (ByteArray) -> Unit): UIImagePickerController {
        onImagePicked = callback
        return UIImagePickerController().apply {
            pickerController = this
            setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
            setDelegate(this@IosImagePickerDelegate)
        }
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val imageData = UIImageJPEGRepresentation(image, 0.8)?.toByteArray()
            if (imageData != null) {
                onImagePicked?.invoke(imageData)
            }
        }
        picker.dismissViewControllerAnimated(true) {
            this.pickerController = null
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) {
            this.pickerController = null
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val bytes = this.bytes?.reinterpret<ByteVar>()
    val length = this.length.toInt()
    return ByteArray(length) { index ->
        bytes?.get(index) ?: 0
    }
}
