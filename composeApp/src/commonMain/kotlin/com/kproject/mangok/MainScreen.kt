package com.kproject.mangok

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kproject.mangok.data.repository.MangaRepository
import io.kamel.core.Resource
import io.kamel.image.asyncPainterResource

@Composable
fun MainScreen() {
    var images by remember { mutableStateOf(mutableStateListOf<String>()) }
    var chapter by remember { mutableStateOf(0) }
    var viewState: ViewState by rememberSaveable { mutableStateOf(ViewState.Loading) }

    var dataRequested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(dataRequested) {
        if (!dataRequested) {
            viewState = ViewState.Loading
            val mainRepository = MangaRepository()
            val randomChapter = mainRepository.generateRandomChapter()
            if (randomChapter != -1) {
                chapter = randomChapter
                val mangaImages = mainRepository.getImagesFromChapter(randomChapter)
                viewState = if (mangaImages.isNotEmpty()) {
                    images = mangaImages.toMutableStateList()
                    ViewState.Success
                } else {
                    ViewState.Error
                }
            } else {
                viewState = ViewState.Error
            }
            dataRequested = true
        }
    }

    var showLoadNewChapterDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.Center,
    ) {
        when (viewState) {
            ViewState.Loading -> {
                LoadingIndicator()
            }
            ViewState.Success -> {
                MainContent(
                    images = images,
                    chapter = chapter,
                    onLoadNewChapter = {
                        showLoadNewChapterDialog = true
                    }
                )

                NewChapterLoaderDialog(
                    showDialog = showLoadNewChapterDialog,
                    onDismiss = { showLoadNewChapterDialog = false },
                    onClickButtonOk = { dataRequested = false }
                )
            }
            ViewState.Error -> {
                Error(onTryAgain = { dataRequested = false })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    images: List<String>,
    chapter: Int,
    onLoadNewChapter: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { images.size }
    )
    var showTopBarAndActions by rememberSaveable { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showTopBarAndActions = !showTopBarAndActions }
    ) {
        VerticalPager(state = pagerState) { page ->
            Box {
                CustomImage(url = images[page])

                AnimatedVisibility(
                    visible = showTopBarAndActions,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    ItemIndexIndicator(
                        currentIndex = page,
                        imageListSize = images.size
                    )
                }

                AnimatedVisibility(
                    visible = showTopBarAndActions,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(0.5f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable {
                                onLoadNewChapter.invoke()
                            }
                    )
                }
            }
        }

        AnimatedVisibility(visible = showTopBarAndActions) {
            TopAppBar(
                title = {
                    Text(text = "Capítulo $chapter")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(0.5f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun CustomImage(url: String) {
    when (val resource = asyncPainterResource(url)) {
        is Resource.Loading -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        is Resource.Success -> {
            val painter = resource.value
            Image(
                painter = painter,
                contentDescription = "Manga image",
                modifier = Modifier
                    .fillMaxSize()
            )
        }
        is Resource.Failure -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF9A0C03))
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFC3C3C3),
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun NewChapterLoaderDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onClickButtonOk: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            content = {
                Column(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 24.dp,
                            bottom = 14.dp
                        )
                ) {
                    Text(
                        text = "Deseja carregar um novo capítulo aleatório?",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancelar",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        TextButton(
                            onClick = {
                                onDismiss.invoke()
                                onClickButtonOk.invoke()
                            },
                        ) {
                            Text(
                                text = "OK",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ItemIndexIndicator(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    imageListSize: Int
) {
    Text(
        text = "${currentIndex + 1} / $imageListSize",
        fontSize = 16.sp,
        color = Color(0xFFFFFFFF),
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondary.copy(0.5f),
                shape = CircleShape
            )
            .padding(8.dp)
    )
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.secondary,
            strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
            modifier = Modifier.size(60.dp)
        )
    }
}

@Composable
private fun Error(onTryAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ops! Parece que deu algum problema ao tentar obter o capítulo.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 20.sp,
            fontWeight = Bold,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onTryAgain) {
            Text(
                text = "Tentar novamente",
                fontSize = 18.sp
            )
        }
    }
}

enum class ViewState {
    Loading,
    Error,
    Success
}