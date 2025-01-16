package dev.valerii.payflo.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIApplication

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val delegate = remember { IosImagePickerDelegate() }

    return {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        val picker = delegate.createPicker(onImagePicked)
        rootController?.presentViewController(picker, true, null)
    }
}