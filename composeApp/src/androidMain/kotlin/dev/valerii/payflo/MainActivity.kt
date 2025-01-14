package dev.valerii.payflo

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.valerii.payflo.picker.AndroidImagePicker
import dev.valerii.payflo.picker.ImagePicker
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin
object ImagePickerHolder {
    private var instance: AndroidImagePicker? = null

    fun setInstance(picker: AndroidImagePicker) {
        instance = picker
    }

    fun getInstance(): AndroidImagePicker? = instance
}

// androidMain
@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to ByteArray
            context.contentResolver.openInputStream(uri)?.use { stream ->
                onImagePicked(stream.readBytes())
            }
        }
    }

    return {
        photoPicker.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }
}

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidApp.context = applicationContext
        /*
        // Create and register the image picker
        val imagePicker = getImagePicker() as AndroidImagePicker
        imagePicker.registerLauncher(this)
        ImagePickerHolder.setInstance(imagePicker)
        */
        setContent {
            App()
        }
    }
    /*
    override fun onResume() {
        super.onResume()
        AndroidActivityProvider.setCurrentActivity(this)
    }

    override fun onPause() {
        super.onPause()
        AndroidActivityProvider.setCurrentActivity(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        getKoin().get<ImagePicker>().let {
            if (it is AndroidImagePicker) {
                it.unregisterLauncher()
            }
        }
    } */
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}