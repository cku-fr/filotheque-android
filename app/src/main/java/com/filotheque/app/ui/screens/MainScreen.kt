package com.filotheque.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filotheque.app.data.local.entity.ThreadEntity
import com.filotheque.app.ui.components.AddThreadSheet
import com.filotheque.app.ui.components.AddThreadTab
import com.filotheque.app.ui.components.EditCustomThreadSheet
import com.filotheque.app.ui.components.SettingsBackupSheet
import com.filotheque.app.ui.components.ThreadDetailSheet
import com.filotheque.app.ui.viewmodel.BrandFilter
import com.filotheque.app.ui.viewmodel.ChartStatusFilter
import com.filotheque.app.ui.viewmodel.ProjectViewModel
import com.filotheque.app.ui.viewmodel.ThreadViewModel
import kotlinx.coroutines.launch

enum class MainTab(val label: String) {
    INVENTORY("Inventaire"),
    CHARTS("Nuanciers"),
    SHOPPING("Courses"),
    BRACELETS("Bracelets")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    threadViewModel: ThreadViewModel,
    projectViewModel: ProjectViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainTab.INVENTORY) }
    var showAddThreadSheet by remember { mutableStateOf(false) }
    var editingCustomThread by remember { mutableStateOf<ThreadEntity?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val filter by threadViewModel.filter.collectAsState()
    val allThreads by threadViewModel.allThreads.collectAsState()
    val inventoryThreads by threadViewModel.inventoryThreads.collectAsState()
    val chartThreads by threadViewModel.chartThreads.collectAsState()
    val shoppingList by threadViewModel.shoppingList.collectAsState()
    val selectedThread by threadViewModel.selectedThread.collectAsState()
    val equivalents by threadViewModel.equivalents.collectAsState()
    val closestDmc by threadViewModel.closestDmcSuggestion.collectAsState()
    val snackbarMessage by threadViewModel.snackbarMessage.collectAsState()

    val projects by projectViewModel.projects.collectAsState()
    val selectedProjectId by projectViewModel.selectedProjectId.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            threadViewModel.clearSnackbar()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val json = threadViewModel.exportBackupJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray())
            }
            snackbarHostState.showSnackbar("Sauvegarde exportée !")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (raw != null) {
            threadViewModel.importBackupJson(raw)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            text = when (currentTab) {
                                MainTab.INVENTORY -> "Ma Boîte à Fils"
                                MainTab.CHARTS -> "Nuanciers"
                                MainTab.SHOPPING -> "Liste de courses"
                                MainTab.BRACELETS -> "Mes Bracelets"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Statistiques et sauvegarde")
                        }
                    }
                )
                if (currentTab == MainTab.INVENTORY || currentTab == MainTab.CHARTS) {
                    OutlinedTextField(
                        value = filter.searchQuery,
                        onValueChange = threadViewModel::setSearchQuery,
                        placeholder = {
                            Text(
                                text = "Code (310, E334...) ou nom",
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (filter.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { threadViewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == MainTab.INVENTORY || currentTab == MainTab.CHARTS) {
                ExtendedFloatingActionButton(
                    onClick = { showAddThreadSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Ajouter") }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = currentTab == MainTab.INVENTORY,
                    onClick = { currentTab = MainTab.INVENTORY },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                    label = { Text(MainTab.INVENTORY.label) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.CHARTS,
                    onClick = { currentTab = MainTab.CHARTS },
                    icon = { Icon(Icons.Default.ColorLens, contentDescription = null) },
                    label = { Text(MainTab.CHARTS.label) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.SHOPPING,
                    onClick = { currentTab = MainTab.SHOPPING },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (shoppingList.isNotEmpty()) {
                                    Badge { Text(shoppingList.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        }
                    },
                    label = { Text(MainTab.SHOPPING.label) }
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.BRACELETS,
                    onClick = { currentTab = MainTab.BRACELETS },
                    icon = { Icon(Icons.Default.Style, contentDescription = null) },
                    label = { Text(MainTab.BRACELETS.label) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.INVENTORY -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(BrandFilter.entries) { brand ->
                            FilterChip(
                                selected = filter.brandFilter == brand,
                                onClick = { threadViewModel.setBrandFilter(brand) },
                                label = { Text(brand.label) }
                            )
                        }
                    }
                    ThreadCollectionView(
                        threads = inventoryThreads,
                        viewMode = filter.viewMode,
                        sortMode = filter.sortMode,
                        selectedColorFamily = filter.colorFamily,
                        emptyMessage = "Votre boîte est vide. Ajoutez vos fils depuis Nuanciers ou via le bouton + Ajouter !",
                        onSelectColorFamily = threadViewModel::setColorFamily,
                        onToggleViewMode = threadViewModel::toggleViewMode,
                        onToggleSortMode = threadViewModel::toggleSortMode,
                        onThreadClick = threadViewModel::selectThread,
                        onAdjustQuantity = threadViewModel::adjustQuantity
                    )
                }

                MainTab.CHARTS -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(BrandFilter.DMC, BrandFilter.ANCHOR, BrandFilter.CUSTOM).forEach { brand ->
                            FilterChip(
                                selected = filter.chartBrand == brand,
                                onClick = { threadViewModel.setChartBrand(brand) },
                                label = { Text(brand.label) }
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ChartStatusFilter.entries) { st ->
                            FilterChip(
                                selected = filter.chartStatus == st,
                                onClick = { threadViewModel.setChartStatus(st) },
                                label = { Text(st.label, fontSize = 12.sp) }
                            )
                        }
                    }
                    ThreadCollectionView(
                        threads = chartThreads,
                        viewMode = filter.viewMode,
                        sortMode = filter.sortMode,
                        selectedColorFamily = filter.colorFamily,
                        emptyMessage = "Aucun fil trouvé pour ces critères.",
                        onSelectColorFamily = threadViewModel::setColorFamily,
                        onToggleViewMode = threadViewModel::toggleViewMode,
                        onToggleSortMode = threadViewModel::toggleSortMode,
                        onThreadClick = threadViewModel::selectThread,
                        onAdjustQuantity = threadViewModel::adjustQuantity
                    )
                }

                MainTab.SHOPPING -> {
                    ShoppingListScreen(
                        items = shoppingList,
                        onMarkPurchased = threadViewModel::markPurchased,
                        onRemoveItem = threadViewModel::removeFromShoppingList
                    )
                }

                MainTab.BRACELETS -> {
                    BraceletsScreen(
                        projects = projects,
                        allThreads = allThreads,
                        selectedProjectId = selectedProjectId,
                        onSelectProject = projectViewModel::selectProject,
                        onCreateProject = projectViewModel::createProject,
                        onSyncProjectThreads = projectViewModel::syncProjectThreads,
                        onRemoveThreadFromProject = projectViewModel::removeThreadFromProject,
                        onDeleteProject = { projectViewModel.deleteProject(it.project) },
                        onAddMissingToShoppingList = { project ->
                            projectViewModel.addMissingToShoppingList(project) { count ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("$count fil(s) ajouté(s) aux courses !")
                                }
                            }
                        },
                        onOpenThreadDetail = threadViewModel::selectThread
                    )
                }
            }
        }
    }

    selectedThread?.let { thread ->
        ThreadDetailSheet(
            thread = thread,
            equivalents = equivalents,
            onDismiss = { threadViewModel.selectThread(null) },
            onAdjustQuantity = { delta -> threadViewModel.adjustQuantity(thread.id, delta) },
            onToggleShoppingList = { threadViewModel.toggleShoppingList(thread) },
            onUpdateNote = { note -> threadViewModel.updateNote(thread.id, note) },
            onEditCustomThread = {
                editingCustomThread = thread
                threadViewModel.selectThread(null)
            },
            onDeleteCustomThread = { threadViewModel.deleteCustomThread(thread) }
        )
    }

    editingCustomThread?.let { thread ->
        EditCustomThreadSheet(
            thread = thread,
            allThreads = allThreads,
            closestDmc = closestDmc,
            onColorChanged = threadViewModel::computeClosestDmc,
            onSave = { brand, code, name, hex, isGlitter ->
                threadViewModel.updateCustomThread(thread, brand, code, name, hex, isGlitter)
            },
            onDismiss = { editingCustomThread = null }
        )
    }

    if (showAddThreadSheet) {
        val activeBrand = if (currentTab == MainTab.CHARTS) filter.chartBrand else filter.brandFilter
        val initialTab = if (activeBrand == BrandFilter.CUSTOM) AddThreadTab.CUSTOM else AddThreadTab.CODES
        val initialBrandId = if (activeBrand == BrandFilter.ANCHOR) "ANCHOR" else "DMC"

        AddThreadSheet(
            allThreads = allThreads,
            closestDmc = closestDmc,
            initialTab = initialTab,
            initialBrandId = initialBrandId,
            onColorChanged = threadViewModel::computeClosestDmc,
            onBatchSubmit = threadViewModel::batchAddByCodes,
            onPhotoThreadSelected = { thread -> threadViewModel.selectThread(thread) },
            onCustomSave = threadViewModel::addCustomThread,
            onDismiss = { showAddThreadSheet = false }
        )
    }

    if (showSettingsSheet) {
        SettingsBackupSheet(
            allThreads = allThreads,
            onDismiss = { showSettingsSheet = false },
            onExportClick = { exportLauncher.launch("filotheque-sauvegarde.json") },
            onImportClick = { importLauncher.launch(arrayOf("application/json")) }
        )
    }
}
