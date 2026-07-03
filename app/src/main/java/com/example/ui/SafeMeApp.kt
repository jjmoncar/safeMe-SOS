package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import java.util.Locale
import com.example.data.EmergencyNumber
import com.example.data.EmergencyReport
import com.example.ui.theme.*
import com.example.viewmodel.EmergencyViewModel
import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeMeApp(viewModel: EmergencyViewModel, activity: android.app.Activity? = null) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeReport by viewModel.activeReport.collectAsState()
    val showSafetyDialog by viewModel.showSafetyDialog.collectAsState()
    val safetyTimer by viewModel.safetyCheckTimer.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState(initial = null)

    // Request permissions for location and audio recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!fineGranted) {
            Toast.makeText(context, "Se recomienda permiso de GPS para coordenadas exactas.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    Scaffold(
        bottomBar = {
            if (currentScreen != EmergencyViewModel.Screen.REGISTRATION) {
                SafeMeBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) },
                    hasActiveAlert = activeReport != null
                )
            }
        },
        containerColor = SlateBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    EmergencyViewModel.Screen.REGISTRATION -> {
                        RegistrationScreen(viewModel = viewModel)
                    }
                    EmergencyViewModel.Screen.EMERGENCY_BUTTON -> {
                        EmergencyButtonScreen(viewModel = viewModel, activity = activity)
                    }
                    EmergencyViewModel.Screen.REPORTS_DAILY -> {
                        ReportsScreen(viewModel = viewModel, activity = activity)
                    }
                    EmergencyViewModel.Screen.NUMBERS_DB -> {
                        NumbersDbScreen(viewModel = viewModel)
                    }
                    EmergencyViewModel.Screen.PROFILE -> {
                        ProfileScreen(viewModel = viewModel)
                    }
                    EmergencyViewModel.Screen.HELP -> {
                        HelpScreen(viewModel = viewModel)
                    }
                }
            }

            // Global Safety Check Dialog (Are you still okay?)
            if (showSafetyDialog) {
                AlertDialog(
                    onDismissRequest = { /* Force response */ },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = "Alerta", tint = AmberWarning)
                            Text(
                                "¡VERIFICACIÓN DE SEGURIDAD!",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Ha transcurrido cierto tiempo desde tu reporte de emergencia. ¿Sigues en condiciones de responder?",
                                color = TextWhite,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(80.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = RedEmergency,
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                    }
                            ) {
                                Text(
                                    text = "${safetyTimer ?: 0}s",
                                    color = RedEmergencyLight,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Si no respondes, tu alerta pasará automáticamente a estado NEGRO (Dispositivo apagado o sin señal)",
                                color = TextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissSafetyCheck() },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSafe),
                            modifier = Modifier.testTag("confirm_ok_button")
                        ) {
                            Text("SÍ, ESTOY BIEN", color = TextWhite)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.simulateDisconnection() },
                            modifier = Modifier.testTag("simulate_lost_signal_button")
                        ) {
                            Text("SIMULAR PÉRDIDA DE SEÑAL", color = RedEmergencyLight)
                        }
                    },
                    containerColor = SlateSurface,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            // Simulated incoming alerts for contacts who have the app
            val simulatedAlerts by viewModel.simulatedIncomingAlerts.collectAsState()
            if (simulatedAlerts.isNotEmpty()) {
                val currentAlert = simulatedAlerts.first()
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSimulatedAlert(currentAlert) },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = RedEmergencyLight)
                            Text(
                                stringResource(R.string.simulated_incoming_alert_title),
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateBackground, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.simulated_incoming_alert_desc, currentAlert.contactPhone),
                                color = TextWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // Phone screen mock representation
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RedEmergencyLight, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = RedEmergencyLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            "SAFEME-SOS REALTIME",
                                            color = RedEmergencyLight,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Severity Badge
                                    val severityBg = when (currentAlert.severity) {
                                        "YELLOW" -> AmberWarning
                                        "ORANGE" -> OrangeWarning
                                        "RED" -> RedEmergency
                                        else -> RedEmergencyDark
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(severityBg, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "CÓDIGO ${currentAlert.severity}",
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Content rows
                                    Text(
                                        text = stringResource(R.string.sender_label, currentAlert.senderName),
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Text(
                                        text = stringResource(R.string.contact_label, currentAlert.contactPhone),
                                        color = TextGray,
                                        fontSize = 12.sp
                                    )

                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SlateSurface))

                                    Text(
                                        text = stringResource(R.string.incident_label, currentAlert.alertType),
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = stringResource(R.string.gps_coords_label, String.format(Locale.US, "%.5f", currentAlert.latitude), String.format(Locale.US, "%.5f", currentAlert.longitude)),
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissSimulatedAlert(currentAlert) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedEmergency),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.dismiss_button), color = TextWhite)
                        }
                    },
                    containerColor = SlateSurface,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    }
}

// Bottom Navigation Bar with consistent indicators and emergency state glow
@Composable
fun SafeMeBottomBar(
    currentScreen: EmergencyViewModel.Screen,
    onNavigate: (EmergencyViewModel.Screen) -> Unit,
    hasActiveAlert: Boolean
) {
    val m3NavColors = NavigationBarItemDefaults.colors(
        selectedIconColor = Color(0xFF1D192B),
        unselectedIconColor = TextGray,
        selectedTextColor = M3Purple,
        unselectedTextColor = TextGray,
        indicatorColor = Color(0xFFE8DEF8)
    )

    NavigationBar(
        containerColor = SlateSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val sosColor = if (hasActiveAlert) RedEmergencyLight else RedEmergency

        NavigationBarItem(
            selected = currentScreen == EmergencyViewModel.Screen.EMERGENCY_BUTTON,
            onClick = { onNavigate(EmergencyViewModel.Screen.EMERGENCY_BUTTON) },
            icon = {
                Box(contentAlignment = Alignment.Center) {
                    if (hasActiveAlert) {
                        // Flashing circle behind SOS tab when active
                        val infiniteTransition = rememberInfiniteTransition(label = "SosFlashing")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.6f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "SosScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .drawBehind {
                                    drawCircle(color = RedEmergency.copy(alpha = 0.4f), radius = size.minDimension / 2 * scale)
                                }
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Emergency,
                        contentDescription = "SOS"
                    )
                }
            },
            label = { Text(stringResource(R.string.tab_sos), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = m3NavColors,
            modifier = Modifier.testTag("tab_sos")
        )

        NavigationBarItem(
            selected = currentScreen == EmergencyViewModel.Screen.REPORTS_DAILY,
            onClick = { onNavigate(EmergencyViewModel.Screen.REPORTS_DAILY) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Reportes"
                )
            },
            label = { Text(stringResource(R.string.tab_reports), fontSize = 11.sp) },
            colors = m3NavColors,
            modifier = Modifier.testTag("tab_reports")
        )

        NavigationBarItem(
            selected = currentScreen == EmergencyViewModel.Screen.NUMBERS_DB,
            onClick = { onNavigate(EmergencyViewModel.Screen.NUMBERS_DB) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.LocalHospital,
                    contentDescription = "Emergencias"
                )
            },
            label = { Text(stringResource(R.string.tab_numbers), fontSize = 11.sp) },
            colors = m3NavColors,
            modifier = Modifier.testTag("tab_numbers")
        )

        NavigationBarItem(
            selected = currentScreen == EmergencyViewModel.Screen.PROFILE,
            onClick = { onNavigate(EmergencyViewModel.Screen.PROFILE) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Mi Perfil"
                )
            },
            label = { Text(stringResource(R.string.tab_profile), fontSize = 11.sp) },
            colors = m3NavColors,
            modifier = Modifier.testTag("tab_profile")
        )

        NavigationBarItem(
            selected = currentScreen == EmergencyViewModel.Screen.HELP,
            onClick = { onNavigate(EmergencyViewModel.Screen.HELP) },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Help,
                    contentDescription = "Ayuda"
                )
            },
            label = { Text(stringResource(R.string.tab_help), fontSize = 11.sp) },
            colors = m3NavColors,
            modifier = Modifier.testTag("tab_help")
        )
    }
}

// ----------------- REGISTRATION SCREEN -----------------
@Composable
fun RegistrationScreen(viewModel: EmergencyViewModel) {
    val profile by viewModel.userProfile.collectAsState(initial = null)

    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var identification by remember(profile) { mutableStateOf(profile?.identification ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var familyPhones by remember(profile) { mutableStateOf(profile?.familyPhones ?: "") }
    var installedAppPhonesSet by remember(profile) {
        mutableStateOf(
            profile?.installedAppPhones?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
        )
    }
    var country by remember(profile) { mutableStateOf(profile?.country ?: "Colombia") }
    var city by remember(profile) { mutableStateOf(profile?.city ?: "Bogotá") }

    var expandedCountry by remember { mutableStateOf(false) }
    var expandedCity by remember { mutableStateOf(false) }

    val cities = viewModel.citiesMap[country] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SlateBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic custom decorative Compose-native banner header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RedEmergencyDark, SlateSurface)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = "SafeMe",
                    tint = TextWhite,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SafeMe - SOS",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    "Registro de Seguridad Personal",
                    color = TextGrayLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Datos del Usuario",
                    color = RedEmergencyLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = RedEmergencyLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = RedEmergencyLight,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = RedEmergencyLight,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_name")
                )

                OutlinedTextField(
                    value = identification,
                    onValueChange = { identification = it },
                    label = { Text("Identificación / Cédula") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = RedEmergencyLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = RedEmergencyLight,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = RedEmergencyLight,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_id")
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = RedEmergencyLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = RedEmergencyLight,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = RedEmergencyLight,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                Text(
                    stringResource(R.string.emergency_contacts_title),
                    color = RedEmergencyLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = familyPhones,
                    onValueChange = { familyPhones = it },
                    label = { Text(stringResource(R.string.family_phones_label)) },
                    placeholder = { Text(stringResource(R.string.family_phones_placeholder)) },
                    leadingIcon = { Icon(Icons.Filled.People, contentDescription = null, tint = RedEmergencyLight) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = RedEmergencyLight,
                        unfocusedBorderColor = TextGray,
                        focusedLabelColor = RedEmergencyLight,
                        unfocusedLabelColor = TextGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_family_emails")
                )

                val phonesList = remember(familyPhones) {
                    familyPhones.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }

                if (phonesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.family_phones_app_installed_label),
                        color = RedEmergencyLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    phonesList.forEach { phone ->
                        val isInstalled = installedAppPhonesSet.contains(phone)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SlateCard, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📱 $phone",
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isInstalled) "SafeMe-SOS " + stringResource(R.string.status_online) else "Sin App",
                                    color = if (isInstalled) GreenSafe else TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Checkbox(
                                    checked = isInstalled,
                                    onCheckedChange = { checked ->
                                        val newSet = installedAppPhonesSet.toMutableSet()
                                        if (checked) {
                                            newSet.add(phone)
                                        } else {
                                            newSet.remove(phone)
                                        }
                                        installedAppPhonesSet = newSet
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = RedEmergencyLight,
                                        uncheckedColor = TextGray,
                                        checkmarkColor = TextWhite
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Text(
                    "Ubicación de Rescate",
                    color = RedEmergencyLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Country Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCountry = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_country_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = BorderStroke(1.dp, TextGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Public, contentDescription = null, tint = RedEmergencyLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("País: $country")
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextWhite)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCountry,
                        onDismissRequest = { expandedCountry = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(SlateSurface)
                    ) {
                        viewModel.countriesList.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c, color = TextWhite) },
                                onClick = {
                                    country = c
                                    expandedCountry = false
                                    val newCities = viewModel.citiesMap[c] ?: emptyList()
                                    if (newCities.isNotEmpty()) {
                                        city = newCities[0]
                                    }
                                }
                            )
                        }
                    }
                }

                // City Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedCity = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_city_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = BorderStroke(1.dp, TextGray),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocationCity, contentDescription = null, tint = RedEmergencyLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ciudad: $city")
                            }
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = TextWhite)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedCity,
                        onDismissRequest = { expandedCity = false },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(SlateSurface)
                    ) {
                        cities.forEach { ct ->
                            DropdownMenuItem(
                                text = { Text(ct, color = TextWhite) },
                                onClick = {
                                    city = ct
                                    expandedCity = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val isValid = name.isNotBlank() && identification.isNotBlank() && email.isNotBlank() && familyPhones.isNotBlank()
        Button(
            onClick = {
                if (isValid) {
                    val installedAppPhonesStr = installedAppPhonesSet.joinToString(",")
                    viewModel.registerUser(name, identification, email, familyPhones, country, city, installedAppPhonesStr)
                }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = RedEmergency,
                disabledContainerColor = SlateCard
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_registration_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "ACTIVAR SISTEMA DE RESCATE",
                color = if (isValid) TextWhite else TextGray,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ----------------- ESCALATION STATUS INDICATOR (HIGH DENSITY THEME) -----------------
@Composable
fun EscalationStatusIndicator(activeSeverity: String?) {
    val statusText = when (activeSeverity) {
        "YELLOW" -> stringResource(R.string.severity_low)
        "ORANGE" -> stringResource(R.string.severity_medium)
        "RED" -> stringResource(R.string.severity_high)
        "BLACK" -> stringResource(R.string.offline_unresponsive)
        else -> stringResource(R.string.ready_to_report)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("escalation_status_indicator"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.alert_status),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Amarillo
                val isYellow = activeSeverity == "YELLOW"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isYellow) AmberWarning else AmberWarning.copy(alpha = 0.2f))
                        .then(if (isYellow) Modifier.border(2.dp, TextWhite.copy(alpha = 0.6f), RoundedCornerShape(6.dp)) else Modifier)
                )

                // Naranja
                val isOrange = activeSeverity == "ORANGE"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOrange) OrangeWarning else OrangeWarning.copy(alpha = 0.2f))
                        .then(if (isOrange) Modifier.border(2.dp, TextWhite.copy(alpha = 0.6f), RoundedCornerShape(6.dp)) else Modifier)
                )

                // Rojo
                val isRed = activeSeverity == "RED"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRed) Color(0xFFC0392B) else Color(0xFFC0392B).copy(alpha = 0.2f))
                        .then(if (isRed) Modifier.border(2.dp, TextWhite.copy(alpha = 0.6f), RoundedCornerShape(6.dp)) else Modifier)
                )

                // Negro (Sin Señal)
                val isBlack = activeSeverity == "BLACK"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isBlack) Color.Black else Color.Black.copy(alpha = 0.2f))
                        .then(if (isBlack) Modifier.border(1.dp, TextGray.copy(alpha = 0.5f), RoundedCornerShape(6.dp)) else Modifier)
                )

                // Verde (A Salvo / Ready)
                val isGreen = activeSeverity == null || activeSeverity == "GREEN"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGreen) GreenSafe else GreenSafe.copy(alpha = 0.2f))
                        .then(if (isGreen) Modifier.border(2.dp, TextWhite.copy(alpha = 0.6f), RoundedCornerShape(6.dp)) else Modifier)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activeSeverity == null) GreenSafe else if (activeSeverity == "YELLOW") AmberWarning else if (activeSeverity == "ORANGE") OrangeWarning else if (activeSeverity == "RED") Color(0xFFC0392B) else Color.Black)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    color = if (activeSeverity != null) M3Purple else TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ----------------- EMERGENCY BUTTON SCREEN -----------------
@Composable
fun EmergencyButtonScreen(viewModel: EmergencyViewModel, activity: android.app.Activity? = null) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState(initial = null)
    val activeReport by viewModel.activeReport.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val detectedSpeech by viewModel.detectedSpeech.collectAsState()
    val emailLogs by viewModel.emailLogs.collectAsState()
    val dailyReports by viewModel.dailyReports.collectAsState()

    var selectedEventuality by remember { mutableStateOf("Otro Grave") }
    var voiceModeEnabled by remember { mutableStateOf(false) }

    val eventualities = listOf(
        "Terremoto / Sismo",
        "Robo",
        "Asalto / Violencia",
        "Tsunami / Inundación",
        "Ascensor Atrapado",
        "Tapiado / Derrumbe",
        "Incendio",
        "Otro Grave"
    )

    // Speech simulation commands for easy browser/emulator testing
    val mockVoiceCommands = listOf("SafeMe", "Auxilio", "Terremoto", "Asalto", "Ascensor")

    val isCentralDispatchEnabled by viewModel.isCentralDispatchEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(SlateBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCentralDispatchEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("central_dispatch_disabled_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC0392B).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFFC0392B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFECF0F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.banner_dispatch_disabled_title),
                            color = Color(0xFFECF0F1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.banner_dispatch_disabled_desc),
                            color = TextGrayLight,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Welcome bar showing profile stats
        userProfile?.let { profile ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(stringResource(R.string.rescue_monitoring_active), color = TextGray, fontSize = 11.sp)
                    Text(profile.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RedEmergencyLight, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${profile.city}, ${profile.country}", color = TextGrayLight, fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GreenSafe.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GreenSafe)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.status_online), color = GreenSafe, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Escalation Status Indicator Card (Matches design template perfectly)
        EscalationStatusIndicator(activeSeverity = activeReport?.severityColor)

        Spacer(modifier = Modifier.height(12.dp))

        // Disaster selection slider
        Text(
            text = stringResource(R.string.select_emergency_type),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                eventualities.forEach { ev ->
                    val isSelected = selectedEventuality == ev
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) RedEmergency else SlateCard.copy(alpha = 0.4f))
                            .border(
                                1.dp,
                                if (isSelected) RedEmergencyLight else TextGray.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedEventuality = ev }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when {
                                ev.contains("Terremoto") -> Icons.Filled.Warning
                                ev.contains("Robo") || ev.contains("Asalto") -> Icons.Filled.Security
                                ev.contains("Tsunami") -> Icons.Filled.Public
                                ev.contains("Ascensor") -> Icons.Filled.Elevator
                                ev.contains("Tapiado") -> Icons.Filled.Layers
                                ev.contains("Incendio") -> Icons.Filled.LocalFireDepartment
                                else -> Icons.Filled.Emergency
                            }
                            Icon(icon, contentDescription = null, tint = TextWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(ev, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Main emergency tactile button with glowing infinite radar ripple
        Text(
            text = stringResource(R.string.sos_instruction_title),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = stringResource(R.string.sos_instruction_desc),
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Active alarm pulsing severity panel
        activeReport?.let { report ->
            val severityColor = when (report.severityColor) {
                "YELLOW" -> AmberWarning
                "ORANGE" -> OrangeWarning
                "RED" -> RedEmergencyLight
                "BLACK" -> Color.Black
                else -> GreenSafe
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                border = BorderStroke(2.dp, severityColor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.active_security_alert),
                        fontWeight = FontWeight.Bold,
                        color = RedEmergencyLight,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(severityColor)
                        )
                        Text(
                            text = stringResource(R.string.severity_label, report.severityColor),
                            fontWeight = FontWeight.Bold,
                            color = severityColor,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = stringResource(R.string.type_gps_format, report.alertType, report.latitude, report.longitude),
                        color = TextGrayLight,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { 
                                viewModel.markAsSafe()
                                activity?.let { com.example.AdManager.showInterstitialAd(it) {} }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSafe),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("safe_green_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.status_green_caps), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.simulateDisconnection() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lost_signal_button"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TextGray)
                        ) {
                            Icon(Icons.Filled.SignalCellularConnectedNoInternet0Bar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.status_black_caps), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    userProfile?.let { profile ->
                        val contacts = profile.familyPhones.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (contacts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = TextGray.copy(alpha = 0.2f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.send_alert_whatsapp_title),
                                color = TextGrayLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            contacts.forEach { contact ->
                                Button(
                                    onClick = {
                                        val msg = viewModel.generateWhatsAppMessage(report, profile)
                                        viewModel.sendWhatsAppMessage(context, contact, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Green
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .testTag("whatsapp_button_$contact"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextWhite)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.send_whatsapp_to, contact), color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tactical button assembly (High Density Theme Specs)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(240.dp)
                .testTag("sos_emergency_button")
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "RadarRipple")
            val rippleRadius1 by infiniteTransition.animateFloat(
                initialValue = 110f,
                targetValue = 240f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "Ripple1"
            )
            val rippleAlpha1 by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "Alpha1"
            )

            // Radar circular shadows
            Box(
                modifier = Modifier
                    .size(rippleRadius1.dp)
                    .drawBehind {
                        drawCircle(
                            color = if (activeReport != null) RedEmergencyLight.copy(alpha = rippleAlpha1) else RedEmergency.copy(alpha = rippleAlpha1)
                        )
                    }
            )

            // Actual circular button matching the w-56 h-56 High Density design
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(RedEmergency)
                    .clickable { viewModel.triggerEmergency(selectedEventuality) }
                    .border(8.dp, RedEmergencyLight.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = stringResource(R.string.tab_sos),
                        tint = TextWhite,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "S O S",
                        color = TextWhite,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = stringResource(R.string.hold_sos),
                        color = TextWhite.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Voice activation triggers
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = RedEmergencyLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(stringResource(R.string.voice_activation_title), color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(stringResource(R.string.voice_activation_desc), color = TextGray, fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = voiceModeEnabled,
                        onCheckedChange = {
                            voiceModeEnabled = it
                            if (!it) viewModel.stopVoiceListening()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RedEmergencyLight,
                            checkedTrackColor = RedEmergencyDark
                        ),
                        modifier = Modifier.testTag("voice_mode_switch")
                    )
                }

                if (voiceModeEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SlateBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { viewModel.startVoiceListening(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isListening) RedEmergency else SlateCard),
                            modifier = Modifier.testTag("listen_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isListening) stringResource(R.string.voice_listening) else stringResource(R.string.voice_activate_mic), fontSize = 11.sp)
                        }

                        Text(
                            text = if (isListening) stringResource(R.string.voice_say_help) else stringResource(R.string.voice_mic_off),
                            color = if (isListening) AmberWarning else TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (detectedSpeech.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.voice_text_captured, detectedSpeech),
                            color = TextGrayLight,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.voice_simulator_desc),
                        color = TextGray,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        mockVoiceCommands.forEach { cmd ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateCard)
                                    .border(1.dp, TextGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.simulateVoiceCommand(cmd) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(cmd, color = RedEmergencyLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Email Dispatch Status Logs pane
        if (emailLogs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, SlateCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "CONSOLA DE MONITOREO DE DESPACHOS",
                        color = GreenSafe,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        emailLogs.take(5).forEach { log ->
                            Text(
                                log,
                                color = if (log.contains("🚨") || log.contains("💀")) RedEmergencyLight else if (log.contains("✅") || log.contains("📩")) GreenSafe else TextGrayLight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. Daily Reports Preview (Design Theme match)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            border = BorderStroke(1.dp, SlateCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPORTES RECIENTES (HOY)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = M3Purple,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(M3Purple.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (userProfile != null) "${userProfile?.city}, ${userProfile?.country?.take(2)?.uppercase(Locale.ROOT)}" else "GPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = M3Purple
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (dailyReports.isEmpty()) {
                    Text(
                        text = "No hay reportes de emergencia hoy.",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dailyReports.take(2).forEach { r ->
                            val severityColor = when (r.severityColor) {
                                "YELLOW" -> AmberWarning
                                "ORANGE" -> OrangeWarning
                                "RED" -> RedEmergency
                                "BLACK" -> Color.Black
                                else -> GreenSafe
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(severityColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alerta: ${r.alertType} en ${r.city} - ${r.severityColor}",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1.0f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------- REPORTS DAILY TAB SCREEN -----------------
@Composable
fun ReportsScreen(viewModel: EmergencyViewModel, activity: android.app.Activity? = null) {
    val dailyReports by viewModel.dailyReports.collectAsState()
    val profile by viewModel.userProfile.collectAsState(initial = null)

    var selectedReportForDetails by remember { mutableStateOf<EmergencyReport?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Reportes de Emergencias Diarios",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    "Filtrado por tu ubicación actual",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = { viewModel.clearAllReportsData() },
                modifier = Modifier.testTag("clear_reports_button")
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "Limpiar Todo", tint = RedEmergencyLight)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        profile?.let { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RedEmergencyLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mostrando incidentes del día en: ${p.city}, ${p.country}",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (dailyReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No se han emitido reportes el día de hoy.", color = TextGrayLight, fontSize = 14.sp)
                    Text("Prueba activando el botón SOS de la pestaña principal.", color = TextGray, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dailyReports) { report ->
                    ReportItemCard(report = report, onClick = { selectedReportForDetails = report })
                }
            }
        }

        // Details Modal Dialog for Selected Report
        selectedReportForDetails?.let { report ->
            ReportDetailDialog(
                report = report,
                onDismiss = { selectedReportForDetails = null },
                onMarkAsSafe = {
                    viewModel.markAsSafe()
                    selectedReportForDetails = null
                    activity?.let { com.example.AdManager.showInterstitialAd(it) {} }
                },
                onForceBlack = {
                    viewModel.simulateDisconnection()
                    selectedReportForDetails = null
                }
            )
        }
    }
}

@Composable
fun ReportItemCard(report: EmergencyReport, onClick: () -> Unit) {
    val severityColor = when (report.severityColor) {
        "YELLOW" -> AmberWarning
        "ORANGE" -> OrangeWarning
        "RED" -> RedEmergencyLight
        "BLACK" -> Color.Black
        else -> GreenSafe
    }

    val severityText = when (report.severityColor) {
        "YELLOW" -> "Amarillo (Leve / Inicial)"
        "ORANGE" -> "Naranja (Grave)"
        "RED" -> "Rojo (Crítico)"
        "BLACK" -> "Negro (Señal Perdida / Incomunicado)"
        else -> "Verde (A salvo / Rescatado)"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("report_item_${report.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(severityColor)
                    .border(2.dp, TextWhite.copy(alpha = 0.4f), CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        report.alertType,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(report.timestamp)),
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Estado: $severityText",
                    color = severityColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "GPS: ${report.latitude}, ${report.longitude}",
                    color = TextGrayLight,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray)
        }
    }
}

@Composable
fun ReportDetailDialog(
    report: EmergencyReport,
    onDismiss: () -> Unit,
    onMarkAsSafe: () -> Unit,
    onForceBlack: () -> Unit
) {
    val context = LocalContext.current
    val severityColor = when (report.severityColor) {
        "YELLOW" -> AmberWarning
        "ORANGE" -> OrangeWarning
        "RED" -> RedEmergencyLight
        "BLACK" -> Color.Black
        else -> GreenSafe
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Emergency, contentDescription = null, tint = severityColor)
                Text("Detalle de Emergencia", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info block
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tipo:", color = TextGray, fontSize = 13.sp)
                    Text(report.alertType, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hora Reporte:", color = TextGray, fontSize = 13.sp)
                    Text(
                        java.text.SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(java.util.Date(report.timestamp)),
                        color = TextWhite,
                        fontSize = 13.sp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estado Actual:", color = TextGray, fontSize = 13.sp)
                    Text(report.severityColor, color = severityColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Divider(color = SlateCard)

                // Simulated Map Widget
                Text("Ubicación Geográfica (Simulada):", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SlateBackground)
                        .border(1.dp, severityColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RedEmergencyLight, modifier = Modifier.size(24.dp))
                        Text("Lat: ${report.latitude}", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Lng: ${report.longitude}", color = TextWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Ver en Google Maps", color = RedEmergencyLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${report.latitude},${report.longitude}"))
                            context.startActivity(intent)
                        })
                    }
                }

                Divider(color = SlateCard)

                // Safety instructions (Gemini simulation)
                Text("Recomendación de Rescate Inteligente:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateCard)
                        .padding(10.dp)
                ) {
                    val instruction = when (report.alertType) {
                        "Terremoto" -> "No ingrese a estructuras inestables. Mantenga la calma y proteja su cabeza. Use silbatos o golpee tubos si está atrapado."
                        "Robo", "Asalto" -> "No ofrezca resistencia. Aléjese del área peligrosa lo antes posible y busque resguardo policial."
                        "Tsunami" -> "Suba de inmediato a un edificio de concreto resistente o colina. Manténgase alejado de la costa hasta aviso oficial."
                        "Ascensor Atrapado" -> "Llame a la línea de mantenimiento. No intente trepar por el hueco del ascensor. Mantenga la calma."
                        "Tapiado / Derrumbe" -> "Respirar con calma para ahorrar oxígeno. Emitir sonidos rítmicos. Rescatistas: apuntalar estructuras vecinas."
                        else -> "Establezca contacto de tranquilidad con la víctima si es posible. Envíe brigadas médicas con las coordenadas satelitales."
                    }
                    Text(
                        instruction,
                        color = TextGrayLight,
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle(lineHeight = 16.sp)
                    )
                }

                if (report.severityColor != "GREEN") {
                    Divider(color = SlateCard)
                    Text("Modificar Estado del Reporte:", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onMarkAsSafe() },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSafe),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_mark_safe"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("A SALVO (VERDE)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onForceBlack() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("modal_mark_black"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TextGray)
                        ) {
                            Text("SIN SEÑAL (NEGRO)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("CERRAR", color = TextWhite)
            }
        },
        containerColor = SlateSurface,
        shape = RoundedCornerShape(24.dp)
    )
}

// ----------------- EMERGENCY NUMBERS TAB SCREEN -----------------
@Composable
fun NumbersDbScreen(viewModel: EmergencyViewModel) {
    val emergencyNumbers by viewModel.emergencyNumbers.collectAsState()
    val profile by viewModel.userProfile.collectAsState(initial = null)
    val context = LocalContext.current
    val isCentralDispatchEnabled by viewModel.isCentralDispatchEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.numbers_db_title),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            stringResource(R.string.numbers_db_subtitle),
            color = TextGray,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isCentralDispatchEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("numbers_central_dispatch_disabled_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC0392B).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFFC0392B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFECF0F1),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.banner_numbers_disabled_title),
                            color = Color(0xFFECF0F1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.banner_numbers_disabled_desc),
                            color = TextGrayLight,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        profile?.let { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = RedEmergencyLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Números correspondientes a: ${p.country}",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (emergencyNumbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = TextGray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No hay números registrados para este país.", color = TextGrayLight, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(emergencyNumbers) { number ->
                    EmergencyNumberCard(
                        number = number,
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.number}"))
                            context.startActivity(intent)
                        },
                        onMessage = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${number.number}")).apply {
                                putExtra("sms_body", "SOS SafeMe: Necesito auxilio de emergencia. Mi última ubicación reportada es ${profile?.city ?: ""} ${profile?.country ?: ""}.")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyNumberCard(
    number: EmergencyNumber,
    onCall: () -> Unit,
    onMessage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        border = BorderStroke(1.dp, SlateCard)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    number.category,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    number.number,
                    color = RedEmergencyLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onMessage() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SlateCard)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Enviar SMS", tint = TextWhite)
                }

                IconButton(
                    onClick = { onCall() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RedEmergency)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = "Llamar", tint = TextWhite)
                }
            }
        }
    }
}

// ----------------- PROFILE TAB SCREEN -----------------
@Composable
fun ProfileScreen(viewModel: EmergencyViewModel) {
    val profile by viewModel.userProfile.collectAsState(initial = null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.profile_title),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            stringResource(R.string.profile_subtitle),
            color = TextGray,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RedEmergencyLight, RedEmergencyDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = TextWhite, modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        profile?.let { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileFieldRow(label = stringResource(R.string.field_names), value = p.name)
                    ProfileFieldRow(label = stringResource(R.string.field_id), value = p.identification)
                    ProfileFieldRow(label = stringResource(R.string.field_email), value = p.email)
                    ProfileFieldRow(label = stringResource(R.string.field_country), value = p.country)
                    ProfileFieldRow(label = stringResource(R.string.field_city), value = p.city)
                    ProfileFieldRow(label = stringResource(R.string.field_family_contacts), value = p.familyPhones)
                    ProfileFieldRow(
                        label = stringResource(R.string.family_phones_app_installed_label),
                        value = if (p.installedAppPhones.isNotBlank()) {
                            p.installedAppPhones.split(",").joinToString(", ") { "📱 $it" }
                        } else {
                            "Ninguno"
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Edit or Clear Profile Button
        Button(
            onClick = { viewModel.navigateTo(EmergencyViewModel.Screen.REGISTRATION) },
            colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag("edit_profile_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.edit_profile_button).uppercase(Locale.getDefault()), color = TextWhite)
        }
    }
}

@Composable
fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextGray, fontSize = 13.sp)
        Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun HelpScreen(viewModel: EmergencyViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("help_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(RedEmergency.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Help,
                            contentDescription = null,
                            tint = RedEmergencyLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.help_screen_title),
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.help_screen_desc),
                        color = TextGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.help_step_by_step),
                color = TextGrayLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        item {
            HelpStepCard(
                stepNumber = "1",
                title = stringResource(R.string.help_step1_title),
                description = stringResource(R.string.help_step1_desc),
                icon = Icons.Filled.Person,
                iconColor = M3Purple
            )
        }

        item {
            HelpStepCard(
                stepNumber = "2",
                title = stringResource(R.string.help_step2_title),
                description = stringResource(R.string.help_step2_desc),
                icon = Icons.Filled.Emergency,
                iconColor = RedEmergencyLight
            )
        }

        item {
            HelpStepCard(
                stepNumber = "3",
                title = stringResource(R.string.help_step3_title),
                description = stringResource(R.string.help_step3_desc),
                icon = Icons.Filled.Share,
                iconColor = Color(0xFF25D366)
            )
        }

        item {
            HelpStepCard(
                stepNumber = "4",
                title = stringResource(R.string.help_step4_title),
                description = stringResource(R.string.help_step4_desc),
                icon = Icons.Filled.Warning,
                iconColor = AmberWarning
            )
        }

        item {
            HelpStepCard(
                stepNumber = "5",
                title = stringResource(R.string.help_step5_title),
                description = stringResource(R.string.help_step5_desc),
                icon = Icons.Filled.Timer,
                iconColor = OrangeWarning
            )
        }

        item {
            HelpStepCard(
                stepNumber = "6",
                title = stringResource(R.string.help_step6_title),
                description = stringResource(R.string.help_step6_desc),
                icon = Icons.Filled.CheckCircle,
                iconColor = GreenSafe
            )
        }

        item {
            HelpStepCard(
                stepNumber = "7",
                title = stringResource(R.string.help_step7_title),
                description = stringResource(R.string.help_step7_desc),
                icon = Icons.Filled.LocalHospital,
                iconColor = M3Purple
            )
        }
    }
}

@Composable
fun HelpStepCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(SlateCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
