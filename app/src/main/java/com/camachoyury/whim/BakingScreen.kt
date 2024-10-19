package com.camachoyury.whim

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Objects

val prompt = """
       List a few popular cookie recipes using this JSON schema:
       Recipe = {'recipeName': string, 'ingredients': Array<string>, 'instructions': Array<string>}
       Return: Array<Recipe>
""".trimIndent()

val images = arrayOf(
    // Image generated using Gemini from the prompt "cupcake image"
    R.drawable.baked_goods_1,
    // Image generated using Gemini from the prompt "cookies images"
    R.drawable.baked_goods_2,
    // Image generated using Gemini from the prompt "cake images"
    R.drawable.baked_goods_3
)
val imageDescriptions = arrayOf(
    R.string.image1_description,
    R.string.image2_description,
    R.string.image3_description
)


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun BakingScreen(
    bakingViewModel: BakingViewModel = viewModel()
) {
    val selectedImage = remember { mutableIntStateOf(0) }
    val placeholderPrompt = stringResource(R.string.prompt_placeholder)
    val placeholderResult = stringResource(R.string.results_placeholder)
    var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
    var result by rememberSaveable { mutableStateOf(placeholderResult) }
    val uiState by bakingViewModel.uiState.collectAsState()
    val context = LocalContext.current

    val bitmap =
        remember { mutableStateOf(BitmapFactory.decodeResource(context.resources, R.drawable.place_holder_photo)) }

    val file = context.createImageFile()
    val uri = FileProvider.getUriForFile(
        Objects.requireNonNull(context),
        BuildConfig.APPLICATION_ID + ".provider", file
    )

    var capturedImageUri by remember {
        mutableStateOf<Uri>(Uri.EMPTY)
    }
    // Launcer de la camara
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }

    //Permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }


    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(R.string.baking_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
            LazyRow(
                modifier = Modifier.fillMaxWidth()
            ) {

                itemsIndexed(images) { index, image ->
                    var imageModifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .requiredSize(200.dp)
                        .clickable {
                            selectedImage.intValue = index
                            bitmap.value = BitmapFactory.decodeResource(
                                context.resources,
                                images[selectedImage.intValue]
                            )
                        }

                    if (index == selectedImage.intValue) {

                        imageModifier =
                            imageModifier.border(
                                BorderStroke(
                                    4.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                    }
                    Image(
                        painter = painterResource(image),
                        contentDescription = stringResource(imageDescriptions[index]),
                        modifier = imageModifier
                    )
                }

                item {
                    Image(
                        bitmap = bitmap.value.asImageBitmap(),
                        contentDescription = "",

                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                            .requiredSize(200.dp)
                            .clickable {
                                println("TODO: Add image picker")
                                val permissionCheckResult =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.CAMERA
                                    )
                                if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(uri)

                                } else {
                                    // Request a permission
                                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            }
                    )
                }

                if (capturedImageUri.path?.isNotEmpty() == true) {
                    val cameraImage = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            context.contentResolver,
                            capturedImageUri
                        )
                    )
                    bitmap.value = cameraImage

                }
            }

        Row(
            modifier = Modifier.padding(all = 16.dp)
        ) {
            TextField(
                value = prompt,
                label = { Text(stringResource(R.string.label_prompt)) },
                onValueChange = { prompt = it },
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )

            Button(
                onClick = {
                    bakingViewModel.sendPrompt(bitmap.value, prompt)
                },
                enabled = prompt.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Text(text = stringResource(R.string.action_go))
            }
        }
        var textColor = MaterialTheme.colorScheme.onSurface
        when(uiState) {

            UiState.Initial -> {
                Text(
                    text = "recipe",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is UiState.SuccessData -> {
                RecipeDataView((uiState as UiState.SuccessData).recipe)
            }
            is UiState.Error -> {
                Text(
                    text = (uiState as UiState.Error).errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                    color = textColor
                )
            }
            is UiState.Success -> {
                textColor = MaterialTheme.colorScheme.onSurface
                result = (uiState as UiState.Success).outputText
                val scrollState = rememberScrollState()
                Text(
                    text = result,
                    textAlign = TextAlign.Start,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }
            }
        }


//        if (uiState is UiState.Loading) {
//            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//        } else {
//
//            if (uiState is UiState.Error) {
//                textColor = MaterialTheme.colorScheme.error
//                result = (uiState as UiState.Error).errorMessage
//            } else if (uiState is UiState.Success) {
//
//            }
//            val scrollState = rememberScrollState()
//            Text(
//                text = result,
//                textAlign = TextAlign.Start,
//                color = textColor,
//                modifier = Modifier
//                    .align(Alignment.CenterHorizontally)
//                    .padding(16.dp)
//                    .fillMaxSize()
//                    .verticalScroll(scrollState)
//            )
//        }

}

fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    val image = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        externalCacheDir      /* directory */
    )
    return image
}


@Composable
fun RecipeDataView(data: Recipe) {
    data?.let { recipe ->
        Column (
            modifier = Modifier
                .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = recipe.recipeName.orEmpty(), modifier = Modifier
                    .fillMaxWidth(0.85f),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Ingredients",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                itemsIndexed(data.ingredients) { index, ingredient ->
                    Text(
                        text = ingredient,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.Center)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                itemsIndexed(data.instructions) { index, instruction ->
                    Text(
                        text = instruction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.Center)
                            .align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

    }
    }

}
