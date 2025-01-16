package dev.valerii.payflo.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import kotlinx.cinterop.*

class IosImagePickerDelegate : NSObject(), UIImagePickerControllerDelegateProtocol,
    UINavigationControllerDelegateProtocol {
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
