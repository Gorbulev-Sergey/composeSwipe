package ru.gorbulevsv.composeswipe

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.gorbulevsv.composeswipe.ui.theme.ComposeSwipeTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

class MainActivity : ComponentActivity() {
    val countPage = Int.MAX_VALUE
    val centralPage = countPage / 2 + 10
    var isDateDialogShow = mutableStateOf(false)

    var isNewStyle = mutableStateOf(true)
    var isNewStyleText =
        derivedStateOf { if (isNewStyle.value) "новый ст." else "старый ст." }

    @SuppressLint("SetJavaScriptEnabled", "UnrememberedMutableState")
    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeSwipeTheme {
                var pagerState = rememberPagerState(
                    pageCount = { countPage }, initialPage = centralPage
                )
                val coroutineScope = rememberCoroutineScope()
                var isFirstLoad = mutableStateOf(true)
                var step = derivedStateOf { pagerState.currentPage - centralPage }

                var dateCurrent = mutableStateOf(LocalDateTime.now())
                var dateForUrl = derivedStateOf {
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    var date = when {
                        step.value == 0 -> dateCurrent.value
                        else -> dateCurrent.value.plusDays(step.value.toLong())
                    }
                    date.format(formatter)
                }
                var dateForSubTitle = derivedStateOf {
                    val formatterDayOfWeek = DateTimeFormatter.ofPattern("E., ")
                    val formatter =
                        DateTimeFormatter.ofPattern("d MMMM yyyy г. " + if (isNewStyle.value) "(н.ст.)" else "(ст.ст.)")
                    var date = when {
                        step.value == 0 -> dateCurrent.value
                        else -> dateCurrent.value.plusDays(step.value.toLong())
                    }
                    if (isNewStyle.value) {
                        date.format(formatterDayOfWeek) + date.format(formatter)
                    } else {
                        date.format(formatterDayOfWeek) + date.minusDays(13).format(formatter)
                    }
                }
                var dateForBottomPanel = derivedStateOf {
                    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy г." + "(н.ст.)")
                    var date = when {
                        step.value == 0 -> dateCurrent.value
                        else -> dateCurrent.value.plusDays(step.value.toLong())
                    }
                    date.format(formatter)
                }

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
                                    Text(
                                        text = "Богослужебные указания",
                                        fontSize = 20.sp
                                    )
                                    Text(text = dateForSubTitle.value, fontSize = 16.sp)
                                }
                            },
                            actions = {
                                MyButton(
                                    text = isNewStyleText.value,
                                    onClick = { isNewStyle.value = !isNewStyle.value }
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MyButton("Назад", onClick = {
                                    coroutineScope.launch {
                                        pagerState.scrollToPage(pagerState.currentPage - 1)
                                    }
                                })
                                Text(
                                    text = dateForBottomPanel.value,
                                    modifier = Modifier.clickable(true, onClick = {
                                        isDateDialogShow.value = true
                                    })
                                )
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
                        date = dateCurrent.value,
                        pagerState = pagerState,
                        centralPage = centralPage,
                        isFirstLoad = isFirstLoad,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                    if (isDateDialogShow.value) {
                        DatePickerModal(
                            onDateSelected = { v ->
                                if (v != null) {
                                    coroutineScope.launch {
                                        isFirstLoad.value = true
                                        dateCurrent.value = LocalDateTime.parse(
                                            getDateFromLong(v.toLong()),
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                        )
                                        pagerState.scrollToPage(centralPage)
                                    }
                                }
                            },
                            onDismiss = { isDateDialogShow.value = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyPager(
    date: LocalDateTime,
    pagerState: PagerState,
    centralPage: Int,
    isFirstLoad: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = false,
        beyondViewportPageCount = 2
    ) { page ->
        Web(
            url = "http://www.patriarchia.ru/bu/${getDate(page - centralPage, date)}/print.html",
            isFirstLoad = isFirstLoad
        )
    }
}


@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Web(url: String, isFirstLoad: MutableState<Boolean>, modifier: Modifier = Modifier) {
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
                        if (isFirstLoad.value == true) {
                            Thread.sleep(200)
                            isFirstLoad.value = false
                        visibility = View.VISIBLE
                        }
                    }
                }
                //loadUrl(url)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}


fun getDate(step: Int, startDate: LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    var date = when {
        step == 0 -> startDate
        else -> startDate.plusDays(step.toLong())
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

fun getDateFromLong(time: Long): String {
    val date = Date(time)
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    return format.format(date)
}