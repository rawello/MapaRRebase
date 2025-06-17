package com.rawello.mapar.presentation.nodesList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rawello.mapar.data.models.NodeDb

@Composable
fun NodesListContent(nodes: List<NodeDb>, onPointsSelected: (String, String) -> Unit) {
    var selectedPoint1 by remember { mutableStateOf<String?>(null) }
    var selectedPoint2 by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNodes = nodes
        .filter { it.type == "view" }
        .filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        floatingActionButton = {
            if (selectedPoint1 != null && selectedPoint2 != null) {
                FloatingActionButton(
                    onClick = {
                        onPointsSelected(selectedPoint1!!, selectedPoint2!!)
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm")
                }
            }
        },
        modifier = Modifier
            .fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
        ) {
            SearchBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it }
            )
            Text(
                text = if (selectedPoint1 != null && selectedPoint2 != null) {
                    val from = filteredNodes.find { it.id.toString() == selectedPoint1 }?.title ?: selectedPoint1
                    val to = filteredNodes.find { it.id.toString() == selectedPoint2 }?.title ?: selectedPoint2
                    "$from -> $to"
                } else if (selectedPoint1 != null) {
                    val from = filteredNodes.find { it.id.toString() == selectedPoint1 }?.title ?: selectedPoint1
                    "$from -> Куда"
                } else {
                    "Откуда -> Куда"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(filteredNodes.size) { index ->
                    PointCard(
                        headline = filteredNodes[index].title,
                        title = filteredNodes[index].id.toString(),
                        isSelected = filteredNodes[index].id.toString() == selectedPoint1 || filteredNodes[index].id.toString() == selectedPoint2,
                        isFirstSelected = filteredNodes[index].id.toString() == selectedPoint1,
                        onClick = {
                            val idStr = filteredNodes[index].id.toString()
                            if (selectedPoint1 == null) {
                                selectedPoint1 = idStr
                            } else if (selectedPoint2 == null) {
                                if (selectedPoint1 != idStr) {
                                    selectedPoint2 = idStr
                                }
                            } else {
                                selectedPoint1 = idStr
                                selectedPoint2 = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        placeholder = { Text("Поиск...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
    )
}

@Composable
private fun PointCard(
    headline: String,
    title: String,
    isSelected: Boolean,
    isFirstSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isFirstSelected) Color.Gray else Color.LightGray
            } else {
                Color.DarkGray
            },
            contentColor = MaterialTheme.colorScheme.background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = headline,
                    fontSize = 22.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null
            )
        }
    }
}