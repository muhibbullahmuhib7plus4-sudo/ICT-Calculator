package com.example.calculator

/*
SETUP NOTE FOR ME:
- This file replaces app/src/main/java/.../MainActivity.kt in a new Android Studio project made with "Empty Activity" (Compose).
- If the package name at the top does not match my project, I will change only that one package line.
*/

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import java.math.BigInteger
import java.text.DecimalFormat
import kotlin.math.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF2563EB), background = Color(0xFFE1F0FE), surface = Color.White)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFE1F0FE)) { AppRoot() }
            }
        }
    }
}

enum class AppScreen { CALCULATOR, MENU, NUMBER_SYSTEM }
enum class NumberSystemTab(val titleBn: String) { CONVERSION("রূপান্তর"), ARITHMETIC("যোগ ও বিয়োগ"), TWOS_COMPLEMENT("২-এর পরিপূরক") }
enum class NumberBase(val fullName: String, val shortName: String, val radix: Int, val validChars: Set<Char>, val bnName: String) {
    BINARY("Binary", "BIN", 2, setOf('0', '1'), "বাইনারি"),
    OCTAL("Octal", "OCT", 8, ('0'..'7').toSet(), "অক্টাল"),
    DECIMAL("Decimal", "DEC", 10, ('0'..'9').toSet(), "দশমিক"),
    HEXADECIMAL("Hexadecimal", "HEX", 16, (('0'..'9') + ('A'..'F') + ('a'..'f')).toSet(), "হেক্সাডেসিমেল");
    fun isValid(c: Char) = c in validChars || c.uppercaseChar() in validChars
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf(AppScreen.CALCULATOR) }
    var tab by remember { mutableStateOf(NumberSystemTab.CONVERSION) }
    when (screen) {
        AppScreen.CALCULATOR -> CalculatorScreen { screen = AppScreen.MENU }
        AppScreen.MENU -> {
            BackHandler { screen = AppScreen.CALCULATOR }
            UnitConverterMenuScreen({ screen = AppScreen.CALCULATOR }) { tab = NumberSystemTab.CONVERSION; screen = AppScreen.NUMBER_SYSTEM }
        }
        AppScreen.NUMBER_SYSTEM -> {
            BackHandler { screen = AppScreen.MENU }
            NumberSystemScreen(tab, { tab = it }, { screen = AppScreen.MENU })
        }
    }
}

@Composable
fun CalculatorScreen(onOpenMenu: () -> Unit) {
    var expr by remember { mutableStateOf("") }
    var isDone by remember { mutableStateOf(false) }
    var ans by remember { mutableStateOf("") }
    var isSci by remember { mutableStateOf(false) }
    var isDeg by remember { mutableStateOf(true) }

    val parser = remember(isDeg) { CalculatorParser(isDeg) }
    val preview = remember(expr, isDone) {
        if (isDone || expr.isBlank()) ""
        else try {
            val clean = expr.trimEnd('+', '-', '×', '÷', '*', '/', '^', '%', '(', '.')
            if (clean.isNotBlank()) formatDouble(parser.evaluate(clean)) else ""
        } catch (_: Exception) { "" }
    }

    fun onInput(tok: String) {
        if (isDone) { isDone = false; expr = if (tok in listOf("+", "-", "×", "÷", "^", "%")) ans + tok else tok }
        else expr += tok
    }

    fun onBack() {
        if (isDone) { isDone = false; expr = ans.dropLast(1) }
        else if (expr.isNotEmpty()) {
            val f = listOf("sin(", "cos(", "tan(", "log(", "ln(", "√(").firstOrNull { expr.endsWith(it) }
            expr = if (f != null) expr.dropLast(f.length) else expr.dropLast(1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Calculator", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                if (isSci) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFDBEAFE)) {
                        Text(if (isDeg) "DEG" else "RAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8),
                            modifier = Modifier.clickable { isDeg = !isDeg }.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isSci = !isSci }) {
                    Text("Sci", fontSize = 13.sp, color = if (isSci) Color(0xFF2563EB) else Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Switch(checked = isSci, onCheckedChange = { isSci = it }, modifier = Modifier.height(28.dp), colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2563EB)))
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onOpenMenu, modifier = Modifier.size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)) {
                    GridMenuIcon(tint = Color(0xFF1E3A8A))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().weight(1f).shadow(12.dp, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                    if (!isDone) {
                        val txt = if (expr.isEmpty()) "0" else expr
                        Text(txt, fontSize = if (txt.length > 12) 30.sp else 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(6.dp))
                        Text(if (preview.isNotEmpty()) preview else " ", fontSize = 24.sp, color = Color(0xFF64748B).copy(alpha = 0.45f), textAlign = TextAlign.End)
                    } else {
                        Text(expr, fontSize = 16.sp, color = Color(0xFF94A3B8), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(ans, fontSize = if (ans.length > 10) 34.sp else 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                Spacer(Modifier.height(8.dp))

                if (isSci) {
                    val sciRows = listOf(
                        listOf("sin" to "sin(", "cos" to "cos(", "tan" to "tan(", "log" to "log(", "ln" to "ln("),
                        listOf("√" to "√(", "^" to "^", "π" to "π", "e" to "e", "!" to "!"),
                        listOf("(" to "(", ")" to ")", "%" to "%", (if (isDeg) "DEG" else "RAD") to "MODE")
                    )
                    sciRows.forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEach { (lbl, action) ->
                                Box(modifier = Modifier.weight(if (action == "MODE") 2f else 1f).height(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F5F9)).clickable {
                                    if (action == "MODE") isDeg = !isDeg else onInput(action)
                                }, contentAlignment = Alignment.Center) {
                                    Text(lbl, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E3A8A))
                                }
                            }
                        }
                    }
                }

                val keys = listOf(
                    listOf("AC" to 3, "⌫" to 2, "%" to 1, "÷" to 1),
                    listOf("7" to 0, "8" to 0, "9" to 0, "×" to 1),
                    listOf("4" to 0, "5" to 0, "6" to 0, "–" to 1),
                    listOf("1" to 0, "2" to 0, "3" to 0, "+" to 1),
                    listOf("00" to 0, "0" to 0, "." to 0, "=" to 4)
                )
                keys.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (txt, type) ->
                            val bg = when (type) { 1 -> Color(0xFFEFF6FF); 2 -> Color(0xFFF1F5F9); 3 -> Color(0xFFFEE2E2); 4 -> Color(0xFF2563EB); else -> Color(0xFFF8FAFC) }
                            val fg = when (type) { 1 -> Color(0xFF2563EB); 2 -> Color(0xFF334155); 3 -> Color(0xFFDC2626); 4 -> Color.White; else -> Color(0xFF0F172A) }
                            Box(
                                modifier = Modifier.weight(1f).height(52.dp).shadow(if (type == 4) 4.dp else 1.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp)).background(bg).clickable {
                                        when (txt) {
                                            "AC" -> { expr = ""; ans = ""; isDone = false }
                                            "⌫" -> onBack()
                                            "=" -> { if (expr.isNotBlank()) { try { ans = formatDouble(parser.evaluate(expr)); isDone = true } catch (_: Exception) { ans = "Error"; isDone = true } } }
                                            "–" -> onInput("-")
                                            else -> onInput(txt)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(txt, fontSize = if (txt.length > 2) 17.sp else 22.sp, fontWeight = if (type in 1..4) FontWeight.Bold else FontWeight.SemiBold, color = fg)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnitConverterMenuScreen(onBack: () -> Unit, onSelectNumberSystem: () -> Unit) {
    val ctx = LocalContext.current
    val tiles = listOf("Currency", "Length", "Area", "Volume", "Weight", "Temperature", "Speed", "Pressure", "Power", "Number system")
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF1E3A8A))
            }
            Spacer(Modifier.width(12.dp))
            Text("Converters & Tools", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(tiles) { tile ->
                val isNum = tile == "Number system"
                Card(
                    modifier = Modifier.fillMaxWidth().height(105.dp).shadow(4.dp, RoundedCornerShape(18.dp)).clickable {
                        if (isNum) onSelectNumberSystem() else Toast.makeText(ctx, "$tile: Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isNum) Color(0xFFEFF6FF) else Color.White),
                    border = if (isNum) BorderStroke(1.5.dp, Color(0xFF2563EB)) else null
                ) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(if (isNum) Color(0xFF2563EB) else Color(0xFFDBEAFE)), contentAlignment = Alignment.Center) {
                            Text(if (isNum) "01" else tile.take(2).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isNum) Color.White else Color(0xFF1D4ED8))
                        }
                        Column {
                            Text(tile, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (isNum) Color(0xFF1E3A8A) else Color(0xFF0F172A))
                            Text(if (isNum) "Active • ICT Engine" else "Coming soon", fontSize = 11.sp, color = if (isNum) Color(0xFF2563EB) else Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NumberSystemScreen(tab: NumberSystemTab, onTabSelected: (NumberSystemTab) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).background(Color.White, CircleShape).shadow(2.dp, CircleShape)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFF1E3A8A))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("সংখ্যা পদ্ধতি (ICT)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                Text("উচ্চ মাধ্যমিক ICT পাঠ্যবই নির্দেশিকা", fontSize = 11.sp, color = Color(0xFF64748B))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NumberSystemTab.values().forEach { t ->
                val sel = t == tab
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0xFF2563EB) else Color.Transparent)
                        .clickable { onTabSelected(t) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center
                ) {
                    Text(t.titleBn, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = if (sel) Color.White else Color(0xFF475569))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f)) {
            when (tab) {
                NumberSystemTab.CONVERSION -> NumberConversionTab()
                NumberSystemTab.ARITHMETIC -> NumberArithmeticTab()
                NumberSystemTab.TWOS_COMPLEMENT -> TwosComplementTab()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberConversionTab() {
    var inBase by remember { mutableStateOf(NumberBase.DECIMAL) }
    var outBase by remember { mutableStateOf(NumberBase.OCTAL) }
    var input by remember { mutableStateOf("39.05") }
    var isExp by remember { mutableStateOf(false) }
    var showSheetIn by remember { mutableStateOf(false) }
    var showSheetOut by remember { mutableStateOf(false) }

    val conv = remember(input, inBase, outBase) { NumberConverter.convert(input, inBase, outBase) }
    val exp = remember(input, inBase, outBase, conv) { NumberConverter.generateExplanation(input, inBase, outBase, conv) }

    fun onKey(k: String) {
        input = when (k) {
            "AC" -> "0"
            "⌫" -> if (input.length <= 1) "0" else input.dropLast(1)
            "." -> if (!input.contains(".")) "$input." else input
            "00" -> if (input != "0") "${input}00" else input
            else -> if (input == "0") k else "$input$k"
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${inBase.fullName} ${inBase.shortName} >", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showSheetIn = true }.padding(4.dp))
                            Text("Base ${inBase.radix}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                        Text(input, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${outBase.fullName} ${outBase.shortName} >", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A),
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showSheetOut = true }.padding(4.dp))
                            Text("Base ${outBase.radix}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Text(conv.result, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize().shadow(3.dp, RoundedCornerShape(16.dp)).clickable { isExp = !isExp },
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ICT পাঠ্যবই নিয়ম ও ব্যাখ্যা", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                            Text(if (isExp) "সংক্ষিপ্ত কর
