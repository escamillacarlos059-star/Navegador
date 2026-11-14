package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserTab
import com.example.data.BookmarkEntity
import com.example.data.HistoryEntity
import com.example.nativecore.SecurityClassification
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.IncognitoViolet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ShieldEmerald

@Composable
fun BrowserOmniboxHeader(
    activeTab: BrowserTab,
    omniboxText: String,
    isOmniboxFocused: Boolean,
    isBookmarked: Boolean,
    adBlockEnabled: Boolean,
    findInPageVisible: Boolean,
    findQuery: String,
    findMatchesCurrent: Int,
    findMatchesTotal: Int,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onOmniboxChanged: (String) -> Unit,
    onOmniboxFocusChanged: (Boolean) -> Unit,
    onSubmitOmnibox: () -> Unit,
    onReloadOrStop: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenInSystemBrowser: () -> Unit,
    onOpenSecurityInfo: () -> Unit,
    onOpenSuggestion: (String) -> Unit,
    onFindQueryChanged: (String) -> Unit,
    onFindNext: (Boolean) -> Unit,
    onCloseFind: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Omnibox Capsule
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        width = 1.dp,
                        color = if (isOmniboxFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    IconButton(
                        onClick = onOpenSecurityInfo,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("security_badge_button")
                    ) {
                        val (icon, tint, desc) = when {
                            activeTab.isIncognito -> Triple(
                                Icons.Rounded.VisibilityOff,
                                IncognitoViolet,
                                "Modo privado"
                            )
                            activeTab.security == SecurityClassification.SECURE_HTTPS -> Triple(
                                Icons.Rounded.Lock,
                                ShieldEmerald,
                                "Conexión HTTPS segura"
                            )
                            activeTab.security == SecurityClassification.INTERNAL -> Triple(
                                Icons.Rounded.Shield,
                                if (adBlockEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                "Motor C++20"
                            )
                            else -> Triple(
                                Icons.Rounded.WarningAmber,
                                AmberWarning,
                                "Conexión no cifrada"
                            )
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (omniboxText.isEmpty()) {
                            Text(
                                text = "Buscar o escribir URL",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = omniboxText,
                            onValueChange = onOmniboxChanged,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    onSubmitOmnibox()
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { onOmniboxFocusChanged(it.isFocused) }
                                .testTag("omnibox_input")
                        )
                    }

                    if (isOmniboxFocused && omniboxText.isNotEmpty()) {
                        IconButton(
                            onClick = { onOmniboxChanged("") },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("omnibox_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Limpiar texto",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (!activeTab.isHomePage) {
                        if (activeTab.blockedCountOnPage > 0 && adBlockEnabled) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ShieldEmerald.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable(onClick = onOpenSecurityInfo)
                            ) {
                                Text(
                                    text = "${activeTab.blockedCountOnPage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ShieldEmerald,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onReloadOrStop,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("reload_button")
                        ) {
                            Icon(
                                imageVector = if (activeTab.isLoading) Icons.Rounded.Close else Icons.Rounded.Refresh,
                                contentDescription = if (activeTab.isLoading) "Detener carga" else "Recargar página",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            if (!activeTab.isHomePage) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bookmark_star_button")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Quitar marcador" else "Guardar marcador",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenInSystemBrowser,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("header_open_system_browser_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OpenInBrowser,
                        contentDescription = "Abrir en navegador del sistema",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(visible = activeTab.isLoading) {
            LinearProgressIndicator(
                progress = { (activeTab.progress.coerceIn(0, 100)) / 100f },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }

        AnimatedVisibility(visible = findInPageVisible) {
            FindInPageBar(
                query = findQuery,
                currentMatch = findMatchesCurrent,
                totalMatches = findMatchesTotal,
                onQueryChanged = onFindQueryChanged,
                onNext = { onFindNext(true) },
                onPrevious = { onFindNext(false) },
                onClose = onCloseFind
            )
        }

        // Omnibox Suggestions when typing
        val suggestions = if (isOmniboxFocused && omniboxText.isNotBlank()) {
            val q = omniboxText.trim().lowercase()
            val bmMatches = bookmarks.filter {
                it.title.lowercase().contains(q) || it.url.lowercase().contains(q)
            }.take(3).map { Triple(it.title, it.url, true) }
            val histMatches = history.filter {
                (it.title.lowercase().contains(q) || it.url.lowercase().contains(q)) &&
                    bmMatches.none { b -> b.second == it.url }
            }.take(4).map { Triple(it.title, it.url, false) }
            bmMatches + histMatches
        } else {
            emptyList()
        }

        AnimatedVisibility(visible = suggestions.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 6.dp)
            ) {
                items(suggestions) { (title, url, isBm) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                onOpenSuggestion(url)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isBm) Icons.Rounded.Bookmark else Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = url,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    currentMatch: Int,
    totalMatches: Int,
    onQueryChanged: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Buscar en esta página…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChanged,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("find_in_page_input")
            )
        }
        Text(
            text = if (query.isEmpty()) "" else "$currentMatch/$totalMatches",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Coincidencia anterior")
        }
        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Siguiente coincidencia")
        }
        IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cerrar búsqueda")
        }
    }
}

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    isIncognito: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Atrás"
                    )
                }

                IconButton(
                    onClick = onForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Adelante"
                    )
                }

                IconButton(
                    onClick = onHome,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("nav_home_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Inicio"
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenTabs)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("tabs_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                width = 2.dp,
                                color = if (isIncognito) IncognitoViolet else MaterialTheme.colorScheme.onSurface,
                                shape = RoundedCornerShape(7.dp)
                            )
                    ) {
                        Text(
                            text = tabCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncognito) IncognitoViolet else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Menú del navegador"
                    )
                }
            }
        }
    }
}

@Composable
fun BrowserSideRail(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            selected = false,
            onClick = { if (canGoBack) onBack() },
            enabled = canGoBack,
            icon = { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás") },
            label = { Text("Atrás") }
        )
        NavigationRailItem(
            selected = false,
            onClick = { if (canGoForward) onForward() },
            enabled = canGoForward,
            icon = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Adelante") },
            label = { Text("Adelante") }
        )
        NavigationRailItem(
            selected = false,
            onClick = onHome,
            icon = { Icon(Icons.Rounded.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationRailItem(
            selected = false,
            onClick = onOpenTabs,
            icon = { Icon(Icons.Rounded.Public, contentDescription = "Pestañas") },
            label = { Text("$tabCount") }
        )
        NavigationRailItem(
            selected = false,
            onClick = onOpenMenu,
            icon = { Icon(Icons.Rounded.MoreVert, contentDescription = "Menú") },
            label = { Text("Menú") }
        )
    }
}
