package ru.gorbulevsv.composeswipe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.gorbulevsv.composeswipe.ui.theme.ComposeSwipeTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    val countPage = Int.MAX_VALUE
    val centralPage = countPage / 2 + 10
    val currentDate by mutableStateOf(getDate(0))
    val pageUrl by derivedStateOf { "http://www.patriarchia.ru/bu/${currentDate}/print.html" }

    var isNewStyle by mutableStateOf(true)
    var isNewStyleText = derivedStateOf { if (isNewStyle) "новый ст." else "старый ст." }
    var date by mutableStateOf(LocalDateTime.now())
    var subTitle = derivedStateOf {
        val formatter = DateTimeFormatter.ofPattern("E., d MMMM yyyy г.")
        if (isNewStyle) {
            date.format(formatter)
        } else {
            date.minusDays(13).format(formatter)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeSwipeTheme {
                val pagerState = rememberPagerState(
                    pageCount = { countPage }, initialPage = centralPage
                )
                val coroutineScope = rememberCoroutineScope()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            modifier = Modifier.padding(bottom = 0.dp),
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = colorResource(R.color.main),
                                titleContentColor = colorResource(R.color.white)
                            ),
                            title = {
                                Column(verticalArrangement = Arrangement.spacedBy(-3.dp)) {
                                    Text(text = "Богослужебные указания", fontSize = 20.sp)
                                    Text(text = subTitle.value, fontSize = 16.sp)
                                }
                            },
                            actions = {
                                MyButton(
                                    text = isNewStyleText.value,
                                    onClick = { isNewStyle = !isNewStyle }
                                )
                            }
                        )
                    },
                    bottomBar = {
                        BottomAppBar(
                            containerColor = colorResource(R.color.main),
                            contentColor = colorResource(R.color.white)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MyButton("Назад", onClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage - 1)
                                    }
                                })
                                MyButton("Вперёд", onClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage + 1)
                                    }
                                })
                            }
                        }
                    }, modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MyPager(
                        pagerState = pagerState,
                        centralPage = centralPage,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun MyPager(pagerState: PagerState, centralPage: Int, modifier: Modifier = Modifier) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = false,
        beyondViewportPageCount = 2
    ) { page ->
        Web(
            url = "http://www.patriarchia.ru/bu/${getDate(page - centralPage)}/print.html",
            oldDay = getOldDay(page - centralPage)
        )
    }
}


@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Web(url: String, oldDay: String = "", modifier: Modifier = Modifier) {
    var countWebPageLoad = remember { 0 }
    AndroidView(
        modifier = modifier
            .fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onLoadResource(view: WebView?, url: String?) {
                        super.onLoadResource(view, url)
                        visibility = View.INVISIBLE
                        evaluateJavascript(
                            "document.querySelector('.header')?.remove(); document.querySelector('.main h1')?.remove(); document.querySelector('.main br')?.remove();",
                            null
                        )
                        evaluateJavascript(
                            "try {document.querySelectorAll('b')[0].innerHTML = document?.querySelectorAll('b')[0].innerHTML.replace(new RegExp('[0-9]{1,2}. '), '');} catch (e) {}",
                            null
                        )
                        evaluateJavascript(
                            "try {document.querySelectorAll('strong')[0].innerHTML = document?.querySelectorAll('strong')[0].innerHTML.replace(new RegExp('[0-9]{1,2}. '), '');} catch (e) {}",
                            null
                        )
                        evaluateJavascript(
                            "document.querySelectorAll('a').forEach(e=>e.style.color='blue'); document.querySelectorAll('div').forEach(e=>e.style.fontSize='1.16rem'); document.querySelector('.main').style.lineHeight='1.61rem'; document.querySelectorAll('p').forEach(e=>e.style.textIndent='0'); document.querySelector('body').style.margin='0rem'; document.querySelector('body').style.userSelect='none'; document.querySelector('.main').style.overflowWrap='break-word';",
                            null
                        )
                        if (countWebPageLoad == 0) {
                            Thread.sleep(100)
                            countWebPageLoad += 1
                        }
                        visibility = View.VISIBLE
                    }
                }
                loadUrl(url)
            }
        },
        update = {
            it.loadUrl(url)
        }
    )
}

@Composable
fun MyButton(text: String = "", onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(
                R.color.sub_main
            )
        ),
        contentPadding = PaddingValues(8.dp),
        shape = RoundedCornerShape(12)
    ) {
        Text(text)
    }
}

fun getDate(step: Int): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var date = when {
        step == 0 -> LocalDateTime.now()
        else -> LocalDateTime.now().plusDays(step.toLong())
    }
    return date.format(formatter)
}

fun getOldDay(step: Int): String {
    val formatter = DateTimeFormatter.ofPattern("dd")
    var date = when {
        step == 0 -> LocalDateTime.now().minusDays(13)
        else -> LocalDateTime.now().plusDays(step.toLong()).minusDays(13)
    }
    return date.format(formatter)
}