package com.bylazy.smstotelegram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bylazy.smstotelegram.ui.theme.SMSToTelegramTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SMSToTelegramTheme {
                val multiplePermissionsState = rememberMultiplePermissionsState(
                    listOf(
                        android.Manifest.permission.RECEIVE_SMS,
                        android.Manifest.permission.READ_SMS,
                    )
                )
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Greeting(multiplePermissionsState)
                }
            }
        }
    }

}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Greeting(multiplePermissionsState: MultiplePermissionsState) {
    if (multiplePermissionsState.allPermissionsGranted) {
        HomeScreen()
    } else {
        Box(modifier = Modifier.padding(12.dp), 
            contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.greeting_message)) 
                    Spacer(modifier = Modifier.size(20.dp))
                    Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                        Text(text = stringResource(id = R.string.grant))
                    }
                    if (!multiplePermissionsState.shouldShowRationale) {

                        Spacer(modifier = Modifier.size(20.dp))
                        Row {
                            Icon(imageVector = Icons.Default.Warning,
                                contentDescription = "Warning", tint = Color.Red)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(text = stringResource(id = R.string.warning)) 
                        }

                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val mainViewModel = viewModel<MainViewModel>()
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HOME){
        composable(HOME) {
            HomeAppScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(CONV) {
            ConvScreen(navController = navController, mainViewModel = mainViewModel)
        }
        composable(INFO) {
            InfoScreen {navController.popBackStack()}
        }
        composable(HELP) {
            HelpScreen {navController.popBackStack()}
        }
    }

}

@Composable
fun ConvScreen(navController: NavController, mainViewModel: MainViewModel) {
    val isLoading by mainViewModel.conversationsLoading.collectAsState()
    val conversations by mainViewModel.conversations.collectAsState()
    val selected by mainViewModel.selectedFrom
    val scrollStare = rememberLazyListState()
    LaunchedEffect(key1 = Unit) {
        mainViewModel.getConversations()
    }
    if (isLoading) CircularProgressIndicator()
    else {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "Select phone number:")
            Spacer(modifier = Modifier.size(4.dp))
            Divider()
            Spacer(modifier = Modifier.size(4.dp))
            LazyColumn(state = scrollStare, modifier = Modifier.weight(1f)) {
                items(conversations) {
                    ConvListItem(conversation = it,
                        selected = it == selected,
                        onSelect = mainViewModel::selectConvItem)
                }
            }
            Spacer(modifier = Modifier.size(4.dp))
            Divider()
            Spacer(modifier = Modifier.size(4.dp))
            Row {
                Button(onClick = { navController.popBackStack() }) {
                    Text(text = "Cancel")
                }
                Spacer(modifier = Modifier
                    .size(8.dp)
                    .weight(1f))
                Button(onClick = {
                    mainViewModel.selectFromConvList()
                    navController.popBackStack()
                }) {
                    Text(text = "OK")
                }
            }
        }
    }
}

@Composable
fun ConvListItem(conversation: Conversation,
                 selected: Boolean,
                 onSelect: (Conversation) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .border(
            width = 1.dp,
            color = if (selected) Color.Gray
            else MaterialTheme.colors.surface,
            shape = RoundedCornerShape(8.dp)
        )
        .padding(8.dp)
        .clickable { onSelect(conversation) },
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.padding(8.dp)) {
            Icon(imageVector = Icons.Default.MailOutline, contentDescription = "Icon")
        }
        Spacer(modifier = Modifier.size(4.dp))
        Column {
            Text(text = conversation.from, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = conversation.snippet, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HomeAppScreen(navController: NavController,
               mainViewModel: MainViewModel){

    val currentState by mainViewModel.currentState
    val messageToShow by mainViewModel.messageToShow
    val showInitialWarning by mainViewModel.shouldShowWarning.collectAsState(initial = false)

    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()

    val clipboardManager = LocalClipboardManager.current

    val filterFocusRequester = remember {FocusRequester()}
    val prefixFocusRequester = remember {FocusRequester()}
    val botFocusRequester = remember {FocusRequester()}
    val channelFocusRequester = remember {FocusRequester()}

    val focusManager = LocalFocusManager.current

    LaunchedEffect(key1 = showInitialWarning) {
        if (showInitialWarning) {
            navController.navigate(INFO)
            mainViewModel.warningShown()
        }
    }

    LaunchedEffect(key1 = messageToShow) {
        if (messageToShow.isNotBlank())
            scaffoldState.snackbarHostState.showSnackbar(messageToShow)
    }

    Scaffold(scaffoldState = scaffoldState, 
        bottomBar = { BottomAppBar {
            Row(modifier = Modifier.padding(8.dp)) {
                Button(onClick = { navController.navigate(INFO) },
                    shape = RoundedCornerShape(50)) {
                    Icon(painter = painterResource(id = R.drawable.alert_circle), contentDescription = "Info")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Info")
                }
                Spacer(modifier = Modifier
                    .size(12.dp)
                    .weight(1f))
                Button(onClick = { navController.navigate(HELP) },
                    shape = RoundedCornerShape(50)) {
                    Icon(painter = painterResource(id = R.drawable.help_circle), contentDescription = "Help")
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Help")
                }
            }
        }}) {
        Column(modifier = Modifier
            .padding(12.dp)
            .verticalScroll(scrollState)) {
            TextField(value = currentState.phone, onValueChange = {mainViewModel
                .updateCurrentState(currentState.copy(phone = it))},
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "From:")},
                enabled = !currentState.sendall,
                trailingIcon = {Icon(imageVector = Icons.Default.Add,
                    contentDescription = "Select",
                    modifier = Modifier.clickable {
                        if (!currentState.sendall) navController.navigate(CONV)
                    })},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {filterFocusRequester.requestFocus()})
            )
            Text(text = "Enter or select a phone number. You can enter multiple numbers separated by spaces.",
                style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = currentState.sendall, 
                    onCheckedChange = {mainViewModel
                        .updateCurrentState(currentState.copy(sendall = !currentState.sendall))})
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Forward all incoming SMS")
            }
            Spacer(modifier = Modifier.size(4.dp))
            TextField(value = currentState.filter, onValueChange = {mainViewModel
                .updateCurrentState(currentState.copy(filter = it))},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(filterFocusRequester),
                label = { Text(text = "Text filter:")},
                trailingIcon = {Icon(imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier.clickable { mainViewModel
                        .updateCurrentState(currentState.copy(filter = "")) })},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {prefixFocusRequester.requestFocus()})
            )
            Text(text = "Forward only messages containing the specified text",
                style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.size(4.dp))
            TextField(value = currentState.prefix, onValueChange = {mainViewModel
                .updateCurrentState(currentState.copy(prefix = it))},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(prefixFocusRequester),
                label = { Text(text = "Prefix:")},
                trailingIcon = {Icon(imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier.clickable { mainViewModel
                        .updateCurrentState(currentState.copy(prefix = "")) })},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {botFocusRequester.requestFocus()})
            )
            Text(text = "Add prefix to forwarded messages",
                style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.size(12.dp))
            Divider()
            Spacer(modifier = Modifier.size(12.dp))
            TextField(value = currentState.bot, onValueChange = {mainViewModel
                .updateCurrentState(currentState.copy(bot = it))},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(botFocusRequester),
                label = { Text(text = "Bot access token:")},
                trailingIcon = {Icon(painter = painterResource(id = R.drawable.ic_paste),
                    contentDescription = "Paste",
                    modifier = Modifier.clickable {
                        if (clipboardManager.getText() != null) {
                            mainViewModel
                                .updateCurrentState(currentState
                                    .copy(bot = clipboardManager.getText()?.text?:currentState.bot))
                        }
                    })},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {channelFocusRequester.requestFocus()})
            )
            Text(text = "Forward messages via that Bot. Go to the Help section to find out how to get your token",
                style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.size(4.dp))
            TextField(value = currentState.channel, onValueChange = {mainViewModel
                .updateCurrentState(currentState.copy(channel = it))},
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(channelFocusRequester),
                label = { Text(text = "Telegram channel:")},
                trailingIcon = {Icon(painter = painterResource(id = R.drawable.ic_paste),
                    contentDescription = "Paste",
                    modifier = Modifier.clickable {
                        if (clipboardManager.getText() != null) {
                            mainViewModel
                                .updateCurrentState(currentState
                                    .copy(channel = clipboardManager.getText()?.text?:currentState.channel))
                        }
                    })},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {focusManager.clearFocus()})
            )
            Text(text = "Channel to forward messages to",
                style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.size(4.dp))
            if (currentState.tested) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Bot tested successfully")
                    Spacer(modifier = Modifier
                        .size(8.dp)
                        .weight(1f))
                    Checkbox(checked = true, onCheckedChange = {})
                }
            } else {
                Button(onClick = { mainViewModel.testBot() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)) {
                    Text("Send Test Message")
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Divider()
            Spacer(modifier = Modifier.size(12.dp))
            Button(onClick = { mainViewModel
                .startStopForwarding(!currentState.active) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)) {
                Icon(painter = painterResource(id = R.drawable.ic_power_on),
                    contentDescription = "Start Stop",
                    tint = if (currentState.active) Color.Red else Color.Green)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = if (currentState.active) "Stop" else "Start")
            }
            Spacer(modifier = Modifier.size(54.dp))
        }
    }
}

@Composable
fun HelpScreen(onDone: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .verticalScroll(scrollState)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.size(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.help_circle),
                    contentDescription = "Help", tint = Color.Green)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "Help",
                    style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.weight(1f))
                Icon(painter = painterResource(id = R.drawable.help_circle),
                    contentDescription = "Help", tint = Color.Green)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Divider()
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_1_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_1),
                contentDescription = "First Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_2_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_2),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_3),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_2_step_2),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_4),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_3_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_5),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_6),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_4_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_4_step_2),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_33),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_5_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_8),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_9),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_10),
                contentDescription = "Second Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_6_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            SelectionContainer {
                Text(text = stringResource(id = R.string.link),
                    fontStyle = FontStyle.Italic)
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_7_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_link),
                contentDescription = "Next Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_8_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_9_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(8.dp))
            Image(painter = painterResource(id = R.drawable.img_help_11),
                contentDescription = "Next Step",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth)
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = stringResource(id = R.string.help_10_step),
                textAlign = TextAlign.Justify)
            Spacer(modifier = Modifier.size(16.dp))
            Divider()
            Spacer(modifier = Modifier.size(8.dp))
            Button(onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)) {
                Text(text = "OK")
            }
        }
    }
}

@Composable
fun InfoScreen(onDone: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.alert_circle),
                contentDescription = "Warning", tint = Color.Red)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "Security warning!",
                style = MaterialTheme.typography.h5)
            Spacer(modifier = Modifier.weight(1f))
            Icon(painter = painterResource(id = R.drawable.alert_circle),
                contentDescription = "Warning", tint = Color.Red)
        }
        Spacer(modifier = Modifier.size(16.dp))
        Divider()
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.security_warning_1),
            textAlign = TextAlign.Justify)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.security_warning_2),
            textAlign = TextAlign.Justify)
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.security_warning_3),
            textAlign = TextAlign.Justify)
        Spacer(modifier = Modifier.size(8.dp))
        Divider()
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(id = R.string.delete_prompt),
            fontWeight = FontWeight.Bold,
            color = Color.Red, textAlign = TextAlign.Justify)
        Spacer(modifier = Modifier.size(8.dp))
        Divider()
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50)) {
            Text(text = "OK")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SMSToTelegramTheme {
        InfoScreen {}
    }
}

@Preview(showBackground = true)
@Composable
fun HelpPreview() {
    SMSToTelegramTheme {
        HelpScreen {}
    }
}

@Preview(showBackground = true)
@Composable
fun ConvListItemPreview() {
    SMSToTelegramTheme {
        ConvListItem(
            conversation = Conversation("999", "bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla "),
            selected = true,
            onSelect = {})
    }
}