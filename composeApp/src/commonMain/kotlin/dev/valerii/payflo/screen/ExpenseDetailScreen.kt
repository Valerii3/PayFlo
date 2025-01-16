package dev.valerii.payflo.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import dev.valerii.payflo.image.ByteArrayImage
import dev.valerii.payflo.model.BillItem
import dev.valerii.payflo.model.Expense
import dev.valerii.payflo.model.User
import dev.valerii.payflo.repository.GroupRepository
import dev.valerii.payflo.repository.UserRepository
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ExpenseDetailScreen(
    private val expense: Expense,
    private val participants: List<User>
) : Screen, KoinComponent {
    private val groupRepository: GroupRepository by inject()
    private val userRepository: UserRepository by inject()



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var billItems by remember { mutableStateOf<List<BillItem>>(emptyList()) }
        var currentUserId by remember { mutableStateOf<String?>(null) }
        var orderText by remember { mutableStateOf("") }
        var isAnalyzing by remember { mutableStateOf(false) }

        LaunchedEffect(expense.id) {
            currentUserId = userRepository.getSavedCredentials()!!.userId
            if (expense.isBillAttached) {
                billItems = groupRepository.getBillItemsForExpense(expense.id)
            }
        }

        fun handleItemClick(item: BillItem) {
            // Launch a coroutine to handle the API call
            MainScope().launch {
                groupRepository.toggleBillItemAssignment(item.id, currentUserId!!)
                // Refresh bill items
                billItems = groupRepository.getBillItemsForExpense(expense.id)
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(expense.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Expense total
                item {
                    Text(
                        "Total Amount",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "€${expense.amount}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Paid by
                item {
                    val paidByUser = participants.find { it.id == expense.paidById }
                    Text(
                        "Paid by",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        paidByUser?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bill Items section (if available)
                if (expense.isBillAttached && billItems.isNotEmpty()) {

                    // type in
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "What did you order?",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = orderText,
                                    onValueChange = { orderText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("e.g., I had a pizza and a coke") },
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        isAnalyzing = true
                                        MainScope().launch {
                                            try {
                                                groupRepository.assignItemsByDescription(
                                                    orderDescription = orderText,
                                                    billItems = billItems,
                                                    userId = currentUserId!!
                                                )
                                                billItems = groupRepository.getBillItemsForExpense(expense.id)
                                                orderText = ""
                                            } catch (e: Exception) {
                                                println("Error: ${e.message}")
                                            } finally {
                                                isAnalyzing = false
                                            }
                                        }
                                    },
                                    enabled = orderText.isNotBlank() && !isAnalyzing
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Analyze")
                                    }
                                }
                            }

                            if (isAnalyzing) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        Text(
                            "Bill Items",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(billItems) { item ->
                        BillItemRow(
                            item = item,
                            participants = participants,
                            onClick = { handleItemClick(item) }// Pass the participants here
                        )
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }


                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    // Participants
                    item {
                        ParticipantsList(
                            participants = participants,
                            billItems = billItems,
                            paidById = expense.paidById
                        )
                    }
                } else {
                    // Participants with equal split (when no bill items)
                    item {
                        Text(
                            "Participants",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }


                    // List each participant and their share
                    val shareAmount = expense.amount / expense.participantIds.size
                    items(participants.filter { it.id in expense.participantIds }) { participant ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(participant.name)
                            Text(
                                "€$shareAmount",
                                color = if (participant.id == expense.paidById)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun BillItemRow(
        item: BillItem,
        participants: List<User>,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "€${item.totalPrice}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${item.quantity}x €${item.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                // Show profile pictures of participants who ordered this item
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    item.assignedToUserIds.forEach { userId ->
                        participants.find { it.id == userId }?.let { user ->
                            UserProfileImage(
                                user = user,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ParticipantsList(
    participants: List<User>,
    billItems: List<BillItem>,
    paidById: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Participants",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        participants.forEach { participant ->
            val participantItems = billItems.filter { participant.id in it.assignedToUserIds }
            val totalOwed = participantItems.sumOf { item ->
                item.totalPrice / item.assignedToUserIds.size
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    UserProfileImage(
                        user = participant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(participant.name)
                }
                Text(
                    "€$totalOwed",
                    color = if (participant.id == paidById)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun UserProfileImage(
    user: User,
    modifier: Modifier = Modifier
) {
    if (user.profilePicture != null) {
        val imageData = user.profilePicture!!.decodeBase64Bytes()
        ByteArrayImage(
            imageBytes = imageData,
            contentDescription = "Profile Picture",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback if no profile picture
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.first().toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}