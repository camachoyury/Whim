package com.camachoyury.whim

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
  private val _uiState: MutableStateFlow<UiState> =
    MutableStateFlow(UiState.Initial)
  val uiState: StateFlow<UiState> =
    _uiState.asStateFlow()

  private val generativeModel = GenerativeModel(
    modelName = "gemini-1.5-flash-latest",
    apiKey = BuildConfig.apiKey,
    requestOptions = RequestOptions(apiVersion = "v1beta"),
    generationConfig = generationConfig {
      responseMimeType = "application/json"
    }
  )

  fun sendPrompt(
    bitmap: Bitmap,
    prompt: String
  ) {
    _uiState.value = UiState.Loading

    viewModelScope.launch(Dispatchers.IO) {
      try {
        val response = generativeModel.generateContent(
          content {
            image(bitmap)
            text(prompt)
          }
        )
        println("Recipe: ${response.text}")
        val data = Gson().fromJson(response.text, Recipe::class.java)
        println("Recipe: ${data.toString()}")

        response.text?.let { outputContent ->
          _uiState.value = UiState.Success(outputContent)
        }
//
//        data.toString()?.let { outputContent ->
//          _uiState.value = UiState.Success(outputContent)
//        }

//        data?.let { outputContent ->
//          _uiState.value = UiState.SuccessData(outputContent)
//        }

      } catch (e: Exception) {
        _uiState.value = UiState.Error(e.localizedMessage ?: "")
      }
    }
  }
}