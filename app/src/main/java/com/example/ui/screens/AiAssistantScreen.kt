package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.ChatMessage
import com.example.data.api.GeneratedMediaResult
import com.example.data.api.MessageSender
import com.example.data.firebase.BinhMyProductFirestore
import com.example.data.firebase.FirebaseUserState
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AiHubTab(val title: String, val iconEmoji: String) {
    CHAT("Trợ lý Chat", "💬"),
    LIVE_VOICE("Live Voice", "🎙️")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier,
    selectedModel: String = "gemini-3.5-flash",
    onSelectModel: (String) -> Unit = {},
    selectedRole: String = "FARMER",
    onSelectRole: (String) -> Unit = {},
    isGoogleMapsEnabled: Boolean = false,
    onToggleGoogleMaps: (Boolean) -> Unit = {},
    onTranscribeSpeech: ((String) -> Unit) -> Unit = {},
    liveTranscript: String? = null,
    onLiveVoice: (String) -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(AiHubTab.CHAT) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Sub-states for Live Voice tab
    var isListening by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("Bác nông dân ơi, rau muống nước VietGAP luống nào non ngon nhất sáng nay?") }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                text = "Trợ lý AI Bình Mỹ",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = FarmGreenDark
                            )
                        }
                        Text(
                            text = "Hỏi đáp kỹ thuật VietGAP, giá cả & quy trình nông nghiệp",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = FarmGreenDark
                        )
                    }
                },
                actions = {
                    if (currentTab == AiHubTab.CHAT) {
                        IconButton(onClick = onClearChat) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Xóa lịch sử",
                                tint = FarmTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FarmGreenLight.copy(alpha = 0.5f))
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(FarmBackground)
        ) {
            // Category Navigation Tabs
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FarmGreenDark
            ) {
                AiHubTab.values().forEach { tab ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.iconEmoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(tab.title, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }
            }

            // Body depending on selected tab
            when (currentTab) {
                AiHubTab.CHAT -> {
                    // Chat tab with Model selection, Role, and Maps grounding
                    Column(modifier = Modifier.fillMaxSize()) {
                        // AI Model & Role Selector Bar
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                // Roles & Google Maps Grounding
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Vai trò:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FarmTextSecondary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val roles = listOf(
                                            Pair("FARMER", "👨‍🌾 Nông dân"),
                                            Pair("VERIFIER", "🔬 VietGAP"),
                                            Pair("COORDINATOR", "🚚 Điều phối")
                                        )
                                        roles.forEach { (key, label) ->
                                            AssistChip(
                                                onClick = { onSelectRole(key) },
                                                label = {
                                                    Text(
                                                        label,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (selectedRole == key) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedRole == key) FarmGreenDark else FarmTextPrimary
                                                    )
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }

                                    // Maps Grounding Toggle
                                    FilterChip(
                                        selected = isGoogleMapsEnabled,
                                        onClick = { onToggleGoogleMaps(!isGoogleMapsEnabled) },
                                        leadingIcon = { Text("📍", fontSize = 12.sp) },
                                        label = { Text("Maps Grounding", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        // Messages Thread
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                ChatMessageBubble(msg)
                            }
                            if (isLoading) {
                                item {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = FarmGreenDark
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Trợ lý đang xử lý dữ liệu...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = FarmTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Quick Prompts
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val quickQuestions = listOf(
                                "🥬 Giá rau muống VietGAP hôm nay?",
                                "📍 Điểm nhận hàng Pop-up ở đâu?",
                                "🌿 Tiêu chuẩn kiểm định VietGAP?",
                                "📅 Lịch giao rau định kỳ tuần?"
                            )
                            items(quickQuestions) { q ->
                                SuggestionChip(
                                    onClick = { onSendMessage(q) },
                                    label = { Text(q, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Input Box with Transcribe microphone
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Microphone button for speech transcription (gemini-3.5-transcribe)
                                IconButton(
                                    onClick = {
                                        onTranscribeSpeech { transcribedText ->
                                            inputText = transcribedText
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Chuyển giọng nói sang chữ",
                                        tint = FarmGreenDark
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = { Text("Hỏi về rau sạch Bình Mỹ...", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .testTag("input_ai_message"),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FarmGreenDark,
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        if (inputText.isNotBlank()) {
                                            onSendMessage(inputText)
                                            inputText = ""
                                        }
                                    },
                                    enabled = inputText.isNotBlank() && !isLoading,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (inputText.isNotBlank() && !isLoading) FarmGreenDark else Color.LightGray)
                                        .testTag("btn_send_ai_message")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Gửi",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                AiHubTab.LIVE_VOICE -> {
                    // Real-time Voice Conversations (gemini-3.8-live)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = FarmGreenLight.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎙️", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Đàm Thoại Trực Tiếp Thời Gian Thực",
                                            fontWeight = FontWeight.Bold,
                                            color = FarmGreenDark,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "Đường truyền đàm thoại trực tiếp siêu tốc",
                                            fontSize = 12.sp,
                                            color = FarmTextSecondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Nói chuyện trực tiếp với trợ lý nông sản Bình Mỹ. Bạn có thể hỏi về vụ mùa, kỹ thuật sơ chế rau muống hoặc hỏi đường tới các trạm nhận.",
                                    fontSize = 12.sp,
                                    color = FarmTextPrimary
                                )
                            }
                        }

                        // Pulsing Visualizer
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) FarmGreenDark else FarmGreenContainer)
                                    .clickable {
                                        isListening = !isListening
                                        if (isListening) {
                                            onLiveVoice(voiceInputText)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                    contentDescription = "Bắt đầu đàm thoại",
                                    tint = if (isListening) Color.White else FarmGreenDark,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (isListening) "Đang lắng nghe & trao đổi qua Live API..." else "Chạm vào biểu tượng micro để bắt đầu đàm thoại",
                                fontWeight = FontWeight.Medium,
                                color = if (isListening) FarmGreenDark else FarmTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        // Live Response Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Phản hồi Live Voice:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = FarmTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = liveTranscript ?: "Dạ bác nông dân Bình Mỹ nghe rõ lời bạn nói rồi! Lô rau muống nước sáng nay xanh mướt, nước tưới kiểm định an toàn, bạn yên tâm đặt trước nhé!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = FarmTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🌾", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isMapsGrounded) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Text("📍", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Google Maps Grounded",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = timeFormat.format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
