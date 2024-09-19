package com.example.tmdbapp.viewmodel

import androidx.lifecycle.*
import com.example.tmdbapp.models.*
import com.example.tmdbapp.utils.*
import kotlinx.coroutines.*

fun MovieViewModel.fetchMovies() {
  if (isLoading || isLastPage) return
  isLoading = true
  viewModelScope.launch {
    safeApiCall {
      repository.discoverMovies(
        page = currentPage,
        sortBy = _currentSortOption.value.apiValue,
        genres = _filterOptions.value.genres,
        releaseYear = _filterOptions.value.releaseYear,
        minRating = _filterOptions.value.minRating,
      )
    }
  }
}

fun MovieViewModel.fetchPopularMovies() {
  if (isLoading || isLastPage) return
  isLoading = true
  viewModelScope.launch {
    safeApiCall { repository.getPopularMovies(currentPage) }
  }
}

internal fun MovieViewModel.searchMovies(query: String) {
  viewModelScope.launch {
    _uiState.value = MovieUiState.Loading
    safeApiCall { repository.searchMovies(query, 1) }
  }
}

private suspend fun MovieViewModel.safeApiCall(apiCall: suspend () -> Resource<MovieResponse>) {
  try {
    val result = apiCall()
    handleMovieResult(result)
  } catch (e: Exception) {
    _uiState.value = MovieUiState.Error(handleError(e))
  } finally {
    isLoading = false
  }
}

private fun MovieViewModel.handleMovieResult(result: Resource<MovieResponse>) {
  when (result) {
    is Resource.Success -> {
      val newMovies = result.data?.results ?: emptyList()
      val currentMovies =
        if (_uiState.value is MovieUiState.Success && currentPage > 1) {
          (_uiState.value as MovieUiState.Success).movies
        } else {
          emptyList()
        }
      _uiState.value = MovieUiState.Success(currentMovies + newMovies)
      currentPage++
      isLastPage = newMovies.isEmpty()
    }

    is Resource.Error -> {
      _uiState.value = MovieUiState.Error(MovieError.ApiError(result.message ?: "Unknown error"))
    }
  }
}
