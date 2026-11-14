package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddModerator
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.SearchEngineOption
import com.example.data.BookmarkEntity
import com.example.nativecore.NativeEngineDiagnostics
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IncognitoViolet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ShieldEmerald

private val SpeedDialPalette = listOf(
    Color(0xFF00B4D8),
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFF10B981),
    Color(0xFF3B82F6),
    Color(0xFFEC4899)
)

@Composable
fun BrowserStartPage(
    isIncognito: Boolean,
    searchEngine: SearchEngineOption,
    adBlockEnabled: Boolean,
    diagnostics: NativeEngineDiagnostics?,
    bookmarks: List<BookmarkEntity>,
    onSubmitSearch: (String) -> Unit,
    onOpenBookmark: (String) -> Unit,
    onOpenIncognitoTab: () -> Unit,
    onOpenEngineInspector: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenInSystemBrowser: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quickQuery by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        // Subtle geometric radar atmosphere
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.22f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.09f),
                        bgColor,
                        bgColor
                    )
                )
            )
            val radii = listOf(140.dp.toPx(), 230.dp.toPx(), 320.dp.toPx())
            radii.forEach { radius ->
                drawCircle(
                    color = primaryColor.copy(alpha = 0.06f),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 104.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isIncognito) {
                            IncognitoViolet.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onOpenEngineInspector)
                            .testTag("start_engine_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = if (isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = if (isIncognito) IncognitoViolet else ShieldEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val blocked = diagnostics?.totalTrackersBlocked ?: 0L
                            Text(
                                text = if (isIncognito) {
                                    "MODO PRIVADO • C++20 SHIELD"
                                } else if (adBlockEnabled) {
                                    "MOTOR C++20 • $blocked BLOQUEADOS"
                                } else {
                                    "MOTOR C++20 • ESCUDO EN PAUSA"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isIncognito) "VECTRA PRIVADO" else "VECTRA",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isIncognito) {
                            "Sin historial local ni rastreadores al cerrar tus pestañas privadas"
                        } else {
                            "Navegador nativo con núcleo C++20 y motor Web Chromium"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    OutlinedTextField(
                        value = quickQuery,
                        onValueChange = { quickQuery = it },
                        placeholder = {
                            Text(
                                text = "Buscar en ${searchEngine.displayName} o escribir URL…",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (quickQuery.isNotBlank()) {
                                    onSubmitSearch(quickQuery)
                                    quickQuery = ""
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_page_search_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(
                            onClick = { onOpenLibrary(0) },
                            label = { Text("Marcadores") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Bookmarks,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("chip_bookmarks")
                        )
                        AssistChip(
                            onClick = { onOpenLibrary(1) },
                            label = { Text("Historial") },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.testTag("chip_history")
                        )
                        if (!isIncognito) {
                            AssistChip(
                                onClick = onOpenIncognitoTab,
                                label = { Text("Privado") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = IncognitoViolet,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.testTag("chip_incognito")
                            )
                        }
                    }
                }
            }

            if (bookmarks.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "ACCESO RÁPIDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(bookmarks.take(12), key = { it.id }) { bookmark ->
                    SpeedDialTile(
                        bookmark = bookmark,
                        onClick = { onOpenBookmark(bookmark.url) }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenEngineInspector)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyanContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Núcleo Nativo C++20",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val abi = diagnostics?.targetAbi ?: "arm64-v8a"
                                val rules = (diagnostics?.filterRulesCount ?: 0) + (diagnostics?.customRulesCount ?: 0)
                                Text(
                                    text = "$abi • $rules reglas activas",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Rounded.OpenInBrowser,
                            contentDescription = "Abrir en navegador del sistema",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOpenInSystemBrowser)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedDialTile(
    bookmark: BookmarkEntity,
    onClick: () -> Unit
) {
    val accent = SpeedDialPalette[bookmark.accentColorIndex % SpeedDialPalette.size]
    val initials = bookmark.title.trim().take(2).uppercase().ifEmpty { "WB" }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("speed_dial_${bookmark.id}")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
