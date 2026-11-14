package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.browser.BrowserTab
import com.example.browser.ExeInspectionPrompt
import com.example.browser.SearchEngineOption
import com.example.browser.SslErrorPrompt
import com.example.data.BookmarkEntity
import com.example.data.BrowserSettings
import com.example.data.HistoryEntity
import com.example.nativecore.NativeEngineDiagnostics
import com.example.nativecore.SecurityClassification
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.IncognitoViolet
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ShieldEmerald
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsModalSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Pestañas abiertas (${tabs.size})",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNewTab(true) },
                        modifier = Modifier.testTag("new_incognito_tab_button")
                    ) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = IncognitoViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Privada")
                    }
                    Button(
                        onClick = { onNewTab(false) },
                        modifier = Modifier.testTag("new_normal_tab_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nueva")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isSelected = tab.id == activeTabId
                    val borderColor = when {
                        isSelected && tab.isIncognito -> IncognitoViolet
                        isSelected -> MaterialTheme.colorScheme.primary
                        tab.isIncognito -> IncognitoViolet.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectTab(tab.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = if (tab.isIncognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Public,
                                    contentDescription = null,
                                    tint = if (tab.isIncognito) IncognitoViolet else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                IconButton(
                                    onClick = { onCloseTab(tab.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cerrar pestaña",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = tab.title.ifBlank { "Inicio" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (tab.isHomePage) "vectra://home" else tab.url,
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    activeTab: BrowserTab,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onOpenFindInPage: () -> Unit,
    onShareUrl: () -> Unit,
    onOpenInSystemBrowser: () -> Unit,
    onOpenExeInspector: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onOpenEngineInspector: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Opciones de Vectra",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (!activeTab.isHomePage) {
                MenuActionRow(
                    icon = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    title = if (isBookmarked) "Quitar de marcadores" else "Guardar en marcadores",
                    subtitle = activeTab.title,
                    onClick = {
                        onToggleBookmark()
                        onDismiss()
                    }
                )
                MenuActionRow(
                    icon = Icons.Rounded.DesktopWindows,
                    title = "Sitio de escritorio",
                    subtitle = if (activeTab.isDesktopMode) "Activado (User-Agent Linux x86_64)" else "Desactivado (Vista móvil)",
                    trailing = {
                        Switch(checked = activeTab.isDesktopMode, onCheckedChange = { onToggleDesktopMode() })
                    },
                    onClick = onToggleDesktopMode
                )
                MenuActionRow(
                    icon = Icons.Rounded.Search,
                    title = "Buscar en la página",
                    onClick = onOpenFindInPage
                )
                MenuActionRow(
                    icon = Icons.Rounded.Share,
                    title = "Compartir enlace",
                    onClick = onShareUrl
                )
            }

            MenuActionRow(
                icon = Icons.Rounded.OpenInBrowser,
                title = "Abrir en navegador del sistema",
                subtitle = "Transferir URL al navegador predeterminado de Android",
                onClick = onOpenInSystemBrowser
            )
            MenuActionRow(
                icon = Icons.Rounded.Terminal,
                title = "Abrir archivo .exe (Inspector PE C++20)",
                subtitle = "Analizar cabecera Windows PE y enviar a app compatible",
                onClick = onOpenExeInspector
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            MenuActionRow(
                icon = Icons.Rounded.Bookmarks,
                title = "Marcadores",
                onClick = { onOpenLibrary(0) }
            )
            MenuActionRow(
                icon = Icons.Rounded.History,
                title = "Historial",
                onClick = { onOpenLibrary(1) }
            )
            MenuActionRow(
                icon = Icons.Rounded.VisibilityOff,
                title = "Nueva pestaña privada",
                onClick = onNewIncognitoTab
            )
            MenuActionRow(
                icon = Icons.Rounded.Memory,
                title = "Motor C++20 y Escudo de Privacidad",
                subtitle = "Diagnóstico nativo, bloqueador y buscador",
                onClick = onOpenEngineInspector
            )
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryModalSheet(
    initialTab: Int,
    bookmarks: List<BookmarkEntity>,
    history: List<HistoryEntity>,
    onOpenUrl: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var filter by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Marcadores (${bookmarks.size})")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Historial (${history.size})")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text("Filtrar por título o dirección…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                val filtered = bookmarks.filter {
                    filter.isBlank() ||
                        it.title.contains(filter, ignoreCase = true) ||
                        it.url.contains(filter, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    EmptyLibraryState(
                        icon = Icons.Rounded.Bookmarks,
                        message = "Sin marcadores que coincidan.\nGuarda tus sitios favoritos con el botón de marcador."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.heightIn(max = 380.dp)
                    ) {
                        items(filtered, key = { it.id }) { bm ->
                            LibraryRowItem(
                                title = bm.title,
                                subtitle = bm.url,
                                onClick = { onOpenUrl(bm.url) },
                                onDelete = { onDeleteBookmark(bm.id) }
                            )
                        }
                    }
                }
            } else {
                val filtered = history.filter {
                    filter.isBlank() ||
                        it.title.contains(filter, ignoreCase = true) ||
                        it.url.contains(filter, ignoreCase = true)
                }
                if (history.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onClearHistory) {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                tint = CrimsonError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Borrar todo el historial", color = CrimsonError)
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    EmptyLibraryState(
                        icon = Icons.Rounded.History,
                        message = "Tu historial está vacío.\nLas páginas que visites en pestañas normales aparecerán aquí."
                    )
                } else {
                    val dateFmt = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            LibraryRowItem(
                                title = item.title,
                                subtitle = "${item.url} • ${dateFmt.format(Date(item.visitedAt))}",
                                onClick = { onOpenUrl(item.url) },
                                onDelete = { onDeleteHistoryItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryRowItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(icon: ImageVector, message: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineInspectorSheet(
    diagnostics: NativeEngineDiagnostics?,
    settings: BrowserSettings,
    customBlockedDomains: List<String>,
    onToggleAdBlock: (Boolean) -> Unit,
    onToggleStripTrackers: (Boolean) -> Unit,
    onToggleJavascript: (Boolean) -> Unit,
    onSelectSearchEngine: (SearchEngineOption) -> Unit,
    onAddCustomDomain: (String) -> Unit,
    onRemoveCustomDomain: (String) -> Unit,
    onResetStats: () -> Unit,
    onDismiss: () -> Unit
) {
    var domainInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = ShieldEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Motor Nativo C++20", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onResetStats) {
                    Text("Reiniciar contadores")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Native C++20 Telemetry Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val d = diagnostics
                    TelemetryLine("Librería JNI", if (d?.nativeLibraryLoaded == true) "libvectra_core.so ACTIVA" else "Modo JVM")
                    TelemetryLine("Estándar", d?.cppStandard ?: "C++20")
                    TelemetryLine("Compilador", d?.compilerInfo ?: "Clang")
                    TelemetryLine("Arquitectura ABI", d?.targetAbi ?: "arm64-v8a")
                    TelemetryLine("Reglas en memoria", "${(d?.filterRulesCount ?: 0) + (d?.customRulesCount ?: 0)} dominios")
                    TelemetryLine("Peticiones analizadas", "${d?.totalRequestsInspected ?: 0L}")
                    TelemetryLine("Rastreadores bloqueados", "${d?.totalTrackersBlocked ?: 0L}")
                    TelemetryLine("Parámetros limpiados", "${d?.totalParamsStripped ?: 0L}")
                    TelemetryLine("Última latencia lookup", "${d?.lastLookupNanos ?: 0L} ns")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Protección y Motor Web", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            SettingSwitchRow(
                title = "Escudo C++20 contra rastreadores",
                subtitle = "Bloquea anuncios y scripts de telemetría en WebViewClient",
                checked = settings.adBlockEnabled,
                onCheckedChange = onToggleAdBlock
            )
            SettingSwitchRow(
                title = "Limpiar parámetros de rastreo",
                subtitle = "Elimina utm_*, fbclid, gclid al instante en C++",
                checked = settings.stripTrackingParams,
                onCheckedChange = onToggleStripTrackers
            )
            SettingSwitchRow(
                title = "Ejecutar JavaScript",
                subtitle = "Necesario para la mayoría de aplicaciones web modernas",
                checked = settings.javascriptEnabled,
                onCheckedChange = onToggleJavascript
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("Buscador predeterminado", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SearchEngineOption.entries.forEach { option ->
                    FilterChip(
                        selected = settings.searchEngineId == option.id,
                        onClick = { onSelectSearchEngine(option) },
                        label = { Text(option.displayName, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Lista negra personalizada (C++)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    placeholder = { Text("ej. tracker.ejemplo.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (domainInput.isNotBlank()) {
                            onAddCustomDomain(domainInput)
                            domainInput = ""
                        }
                    }
                ) {
                    Text("Bloquear")
                }
            }

            if (customBlockedDomains.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                customBlockedDomains.forEach { domain ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMonoFamily
                        )
                        IconButton(
                            onClick = { onRemoveCustomDomain(domain) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Eliminar dominio", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryLine(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMonoFamily,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SecurityInfoDialog(
    tab: BrowserTab,
    adBlockEnabled: Boolean,
    onOpenEngineInspector: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusText = when (tab.security) {
        SecurityClassification.INTERNAL -> "Página interna de Vectra"
        SecurityClassification.SECURE_HTTPS -> "Conexión HTTPS cifrada y verificada"
        SecurityClassification.INSECURE_HTTP -> "Conexión HTTP sin cifrado"
        SecurityClassification.SUSPICIOUS_PUNYCODE -> "Advertencia: dominio Punycode (xn--)"
        SecurityClassification.IP_LITERAL -> "Conexión directa a dirección IP"
    }
    val statusIcon = when (tab.security) {
        SecurityClassification.SECURE_HTTPS -> Icons.Rounded.Lock
        SecurityClassification.INTERNAL -> Icons.Rounded.Shield
        else -> Icons.Rounded.WarningAmber
    }
    val statusTint = when (tab.security) {
        SecurityClassification.SECURE_HTTPS, SecurityClassification.INTERNAL -> ShieldEmerald
        else -> AmberWarning
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(statusIcon, contentDescription = null, tint = statusTint, modifier = Modifier.size(32.dp))
        },
        title = {
            Text(text = statusText, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (tab.isHomePage) "vectra://home" else tab.url,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (adBlockEnabled) {
                        "Escudo C++20 activo: ${tab.blockedCountOnPage} rastreadores bloqueados en esta página."
                    } else {
                        "Escudo C++20 en pausa."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendido") }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenEngineInspector()
            }) {
                Text("Ver motor C++20")
            }
        }
    )
}

@Composable
fun SslErrorDialog(
    prompt: SslErrorPrompt,
    onGoBackSafe: () -> Unit,
    onProceedAnyway: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onGoBackSafe,
        icon = {
            Icon(
                Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = CrimsonError,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Tu conexión no es privada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(prompt.errorSummary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = prompt.url,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onGoBackSafe,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Volver a un lugar seguro")
            }
        },
        dismissButton = {
            TextButton(onClick = onProceedAnyway) {
                Text("Continuar bajo mi riesgo", color = CrimsonError)
            }
        }
    )
}

@Composable
fun ExeInspectorDialog(
    prompt: ExeInspectionPrompt,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    val pe = prompt.inspection
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = if (pe.isValidPe) ShieldEmerald else AmberWarning,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Inspector Windows PE (C++20)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = prompt.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                TelemetryLine("Tamaño", "${prompt.fileSizeBytes} bytes")
                TelemetryLine("Cabecera PE", if (pe.isValidPe) "Válida (MZ + PE\\0\\0)" else "No válida")
                if (pe.isValidPe) {
                    TelemetryLine("Formato", pe.peFormat)
                    TelemetryLine("Arquitectura CPU", pe.architecture)
                    TelemetryLine("Subsistema", pe.subsystem)
                    TelemetryLine("Secciones COFF", "${pe.sectionsCount}")
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (pe.isValidPe) {
                        "Android no ejecuta binarios Win32 (.exe) de forma nativa sin una capa externa como Winlator o Wine. Puedes enviarlo a una app compatible instalada en el dispositivo."
                    } else {
                        pe.statusDetail
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenExternal) {
                Text("Abrir con app compatible")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
