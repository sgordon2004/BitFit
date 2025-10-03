package com.example.bitfit.ui.theme


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.HealthEntry
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@RequiresApi(Build.VERSION_CODES.O)
private fun java.time.LocalDate.toUtcStartOfDayMillis(): Long =
    this.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()

@RequiresApi(Build.VERSION_CODES.O)
private fun millisToLocalDateUtc(millis: Long): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()

private fun metricColor(metric: String, m3: ColorScheme): Color = when (metric) {
    "diet" -> m3.secondary
    "water" -> m3.primary
    "mood" -> m3.tertiary
    "exercise" -> m3.secondaryContainer
    else -> m3.surfaceVariant
}

// date pretty-printer for ISO yyyy-MM-dd -> "Tue, Oct 2, 2025"
@RequiresApi(Build.VERSION_CODES.O)
private fun String.prettyDate(): String = try {
    val ld = java.time.LocalDate.parse(this)
    ld.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
} catch (_: Throwable) {
    this
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
enum class Tab { LOG, AVERAGES}
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HealthScreen(vm: HealthViewModel) {
    val entries by vm.entries.collectAsState(initial = emptyList())
    var showAdd by rememberSaveable { mutableStateOf(false) }

    // which tab is selected
    var selectedTab by rememberSaveable { mutableStateOf(Tab.LOG) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BitFit") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == Tab.LOG) {
                FloatingActionButton(
                    onClick = { showAdd = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) { Text("+") }
            }

        },
        // bottom navigation bar
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == Tab.LOG,
                    onClick = { selectedTab = Tab.LOG },
                    icon = { Text("📝")},
                    label = { Text("Log")}
                )
                NavigationBarItem(
                    selected = selectedTab == Tab.AVERAGES,
                    onClick = { selectedTab = Tab.AVERAGES },
                    icon = { Text("📊") },
                    label = { Text("Averages") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            Tab.LOG -> {
                val grouped = remember(entries) {
                    entries.groupBy { it.date }
                        .toSortedMap(compareByDescending { it })
                }
                LazyColumn(contentPadding = padding) {
                    grouped.forEach { (dateIso, dayEntries) ->
                        item(key = "day-header-$dateIso") {
                            DaySection(
                                dateIso = dateIso,
                                entries = dayEntries,
                                onDelete = { entry -> vm.delete(entry) },
                                onUpdate = { updated -> vm.updateEntry(updated) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            Tab.AVERAGES -> {
                AveragesTab(
                    modifier = Modifier.padding(padding),
                    entries = entries
                )
            }
        }
    }

    if (showAdd) {
        AddEntryDialog(
            onDismiss = { showAdd = false },
            onSave = { metric, dateIso, value, notes ->
                vm.addEntry(metric, dateIso, value, notes)
                showAdd = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEntryDialog(
    onDismiss: () -> Unit,
    onSave: (metric: String, dateIso: String, value: Int, notes: String?) -> Unit
) {

    // Metric options
    val metrics = listOf(
        "diet" to "Calories",
        "water" to "Cups",
        "mood" to "Mood (1-5)",
        "exercise" to "Minutes"
    )

    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedMetric by rememberSaveable { mutableStateOf(metrics[1].first) } // default "water"

    // Date (default = today)
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val today = remember { java.time.LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today) }

    // Value + notes
    var valueText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    // Derived state
    val unitLabel = metrics.first { it.first == selectedMetric }.second
    val valueInt = valueText.toIntOrNull()
    val canSave = valueInt != null &&
            when (selectedMetric) {
                "mood" -> valueInt in 1..5
                else -> valueInt >= 0
            }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Entry")},
        text = {
            Column {
                // Metric Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded}
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedMetric.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        metrics.forEach { (key, _) ->
                            DropdownMenuItem(
                                text = { Text(key.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedMetric = key
                                    expanded = false
                                    valueText = "" // reset value when switching metric
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Date picker launcher
                OutlinedTextField(
                    value = selectedDate.toString(), // ISO yyyy-MM-dd
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Value input (numeric)
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { s -> valueText = s.filter { it.isDigit() } },
                    label = { Text("Value ($unitLabel)") },
                    placeholder = { Text(when (selectedMetric) {
                        "diet" -> "e.g., 2200"
                        "water" -> "e.g., 8"
                        "mood" -> "1-5"
                        else -> "e.g., 30"
                    }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("How you felt, details, etc.") },
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        selectedMetric,
                        selectedDate.toString(),
                        valueInt!!,
                        notes.ifBlank { null }
                    )
                }
            ) { Text("Save", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcStartOfDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        state.selectedDateMillis?.let { millis ->
                            selectedDate = millisToLocalDateUtc(millis)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK")}
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false}) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    }

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryDialog(
    original: HealthEntry,
    onDismiss: () -> Unit,
    onSave: (metric: String, dateIso: String, value: Int, notes: String?) -> Unit
) {
    val metrics = listOf(
        "diet" to "Calories",
        "water" to "Cups",
        "mood" to "Mood (1-5)",
        "exercise" to "Minutes"
    )

    var expanded by rememberSaveable { mutableStateOf(false) }
    var selectedMetric by rememberSaveable { mutableStateOf(original.metric) } // default "water"
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // parse original date or fallback to today
    val parsed = runCatching { java.time.LocalDate.parse(original.date) }.getOrNull()
    var selectedDate by rememberSaveable { mutableStateOf(parsed ?: java.time.LocalDate.now()) }

    var valueText by rememberSaveable { mutableStateOf(original.value.toString()) }
    var notes by rememberSaveable { mutableStateOf(original.notes.orEmpty()) }

    val unitLabel = (metrics.firstOrNull { it.first == selectedMetric } ?: metrics.first()).second
    val valueInt = valueText.toIntOrNull()
    val canSave = valueInt != null &&
            when (selectedMetric) {
                "mood" -> valueInt in 1..5
                else -> valueInt >= 0
            }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry") },
        text = {
            Column {
                // Metric Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedMetric.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        label = { Text("Metric") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        metrics.forEach { (key, _) ->
                            DropdownMenuItem(
                                text = { Text(key.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedMetric = key
                                    expanded = false
                                    valueText = ""
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Date picker launcher
                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // Value
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { s -> valueText = s.filter { it.isDigit() } },
                    label = { Text("Value ($unitLabel)") },
                    placeholder = { Text(when (selectedMetric) {
                        "diet" -> "e.g., 2200"
                        "water" -> "e.g., 8"
                        "mood" -> "1–5"
                        else -> "e.g., 30"
                    }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = false,
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        selectedMetric,
                        selectedDate.toString(),
                        valueInt!!,
                        notes.ifBlank { null }
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toUtcStartOfDayMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        selectedDate = millisToLocalDateUtc(millis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySection(
    dateIso: String,
    entries: List<HealthEntry>,
    onDelete: (HealthEntry) -> Unit,
    onUpdate: (HealthEntry) -> Unit
) {
    // control whether this day is expanded
    var expanded by rememberSaveable(dateIso) { mutableStateOf(true) }
    val prettyDate = remember(dateIso) { dateIso.prettyDate() }

    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Day header row (toggle)
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                headlineContent = { Text(prettyDate, style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text("${entries.size} entr${if (entries.size == 1) "y" else "ies"}") },
                trailingContent = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 8.dp)
            )

            // Entries for the day
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                    entries.forEach { entry ->
                        EntryRow(entry = entry, onDelete = onDelete, onUpdate = onUpdate)
                        Divider()
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EntryRow(
    entry: HealthEntry,
    onDelete: (HealthEntry) -> Unit,
    onUpdate: (HealthEntry) -> Unit
) {
    // controls per-entry expansion
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    var showConfirm by rememberSaveable(entry.id) { mutableStateOf(false) }
    var showEdit by rememberSaveable(entry.id) { mutableStateOf(false) }

    val m3 = MaterialTheme.colorScheme
    val chipColor = metricColor(entry.metric, m3)
    val chipOnColor = if (chipColor.luminance() < 0.5f) m3.onPrimary else m3.onSurface

    val metricLabel = entry.metric.replaceFirstChar { it.uppercase() }
    val valueText = when (entry.metric) {
        "diet" -> "${entry.value} kcal"
        "water" -> "${entry.value} cups"
        "mood" -> "${entry.value}/5"
        "exercise" -> "${entry.value} min"
        else -> entry.value.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        // colored pill behind the title line
        Surface(
            color = chipColor,
            contentColor = chipOnColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            // Title line: Metric + Value in bold
            Text(
                text = "$metricLabel — $valueText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Collapsible details
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                if (!entry.notes.isNullOrBlank()) {
                    Text(
                        text = entry.notes!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(6.dp))
                }
                // date detail (pretty + raw)
                Text(
                    text = "Date: ${entry.date.prettyDate()} (${entry.date})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Actions row (Delete aligned to end)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showEdit = true }) {Text("Edit") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { showConfirm = true },
                        // subtle "destructive" styling
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }

    // Confirm dialog
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete entry?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirm = false
                        onDelete(entry)
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )

    }

    if (showEdit) {
        EditEntryDialog(
            original = entry,
            onDismiss = { showEdit = false },
            onSave = { metric, dateIso, value, notes ->
                showEdit = false
                onUpdate(
                    entry.copy(
                        metric = metric,
                        date = dateIso,
                        value = value,
                        notes = notes
                    )
                )
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
enum class Window(val label: String, val days: Long?) {  // null = all time
    ALL("All", null), D7("7d", 7), D30("30d", 30), D90("90d", 90)
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AveragesTab(
    entries: List<HealthEntry>,
    modifier: Modifier = Modifier
) {
    // Time window filter
    var window by rememberSaveable { mutableStateOf(Window.ALL) }

    // Filter entries by window
    val now = remember { java.time.LocalDate.now() }
    val filtered = remember(entries, window) {
        if (window.days == null) entries
        else {
            val cutoff = now.minusDays(window.days!!)
            entries.filter {
                runCatching { java.time.LocalDate.parse(it.date) }
                    .getOrNull()
                    ?.isAfter(cutoff.minusDays(1)) == true
            }
        }
    }

    // Compute per-metric average
    val stats: Map<String, Stat> = remember(filtered) {
        filtered.groupBy { it.metric }.mapValues { (_, list) ->
            val count = list.size
            val avg = if (count > 0) list.map { it.value }.average() else 0.0
            Stat(count, avg)
        }
    }

    // Always show the four known metrics; default to 0 if none yet
    val metrics = listOf("diet", "water", "mood", "exercise")

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        Text(
            "Averages",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))

        // Window selector chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Window.values().forEach { w ->
                FilterChip(
                    selected = window == w,
                    onClick = { window = w },
                    label = { Text(w.label) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Cards per metric
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.forEach { metric ->
                val stat = stats[metric] ?: Stat(0, 0.0)
                MetricStatCard(metric = metric, stat = stat)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Small explanatory note
        Text(
            text = if (window.days == null)
                "Showing averages for all time."
            else
                "Showing averages for the last ${window.days} days.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// top-level, under imports or near Window enum
private data class Stat(val count: Int, val avg: Double)

@Composable
private fun MetricStatCard(metric: String, stat: Stat) {
    val m3 = MaterialTheme.colorScheme
    val chipColor = metricColor(metric, m3)
    val chipOn = if (chipColor.luminance() < 0.5f) m3.onPrimary else m3.onSurface

    val title = metric.replaceFirstChar { it.uppercase() }
    val avgText = when (metric) {
        "diet" -> "${"%.1f".format(stat.avg)} kcal"
        "water" -> "${"%.1f".format(stat.avg)} cups"
        "mood" -> "${"%.2f".format(stat.avg)} / 5"
        "exercise" -> "${"%.1f".format(stat.avg)} min"
        else -> "%.1f".format(stat.avg)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Accent chip title
            Surface(color = chipColor, contentColor = chipOn, shape = RoundedCornerShape(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Average: $avgText",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Entries: ${stat.count}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}