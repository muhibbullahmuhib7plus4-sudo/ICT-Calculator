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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    fun isValid(c: Char): Boolean = c in validChars || c.uppercaseChar() in validChars
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
                Card(Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.EFF6FF)) {
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
                            Text(if (isExp) "সংক্ষিপ্ত করুন ▲" else "পূর্ণ ধাপ দেখুন ▼", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                        }
                        Spacer(Modifier.height(4.dp))
                        if (!isExp) {
                            Text(exp.shortSummary, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)
                        } else {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)).padding(vertical = 4.dp))
                            Text(exp.fullExplanation, fontSize = 13.sp, color = Color(0xFF0F172A), lineHeight = 20.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        BaseKeypad(inBase, ::onKey)
    }

    if (showSheetIn) ModalBottomSheet({ showSheetIn = false }, containerColor = Color.White) { ChooseBaseSheet(inBase, { inBase = it; input = NumberConverter.sanitize(input, it); showSheetIn = false }) { showSheetIn = false } }
    if (showSheetOut) ModalBottomSheet({ showSheetOut = false }, containerColor = Color.White) { ChooseBaseSheet(outBase, { outBase = it; showSheetOut = false }) { showSheetOut = false } }
}

@Composable
fun BaseKeypad(base: NumberBase, onKey: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val pad = listOf(listOf("A", "B", "C", "D"), listOf("E", "F", "AC", "⌫"), listOf("7", "8", "9", "00"), listOf("4", "5", "6", "."), listOf("1", "2", "3", "0"))
            pad.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    row.forEach { k ->
                        val en = when (k) { "AC", "⌫", "." -> true; "00", "0" -> base.isValid('0'); else -> base.isValid(k.first()) }
                        Box(
                            modifier = Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(10.dp))
                                .background(if (!en) Color(0xFFF1F5F9) else if (k == "AC") Color(0xFFFEE2E2) else if (k == "⌫") Color(0xFFE2E8F0) else Color(0xFFF8FAFC))
                                .clickable(enabled = en) { onKey(k) }, contentAlignment = Alignment.Center
                        ) {
                            Text(k, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (!en) Color(0xFFCBD5E1) else if (k == "AC") Color(0xFFDC2626) else Color(0xFF0F172A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChooseBaseSheet(sel: NumberBase, onSelect: (NumberBase) -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Text("Choose number system", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        Spacer(Modifier.height(12.dp))
        NumberBase.values().forEach { b ->
            val isS = b == sel
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (isS) Color(0xFFEFF6FF) else Color.Transparent).clickable { onSelect(b) }.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${b.fullName} ${b.shortName} (${b.bnName})", fontSize = 16.sp, fontWeight = if (isS) FontWeight.Bold else FontWeight.Medium, color = if (isS) Color(0xFF2563EB) else Color(0xFF1E293B))
                if (isS) Icon(Icons.Default.Check, null, tint = Color(0xFF2563EB))
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCancel, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)), shape = RoundedCornerShape(12.dp)) {
            Text("Cancel", color = Color(0xFF475569))
        }
    }
}

@Composable
fun NumberArithmeticTab() {
    var base by remember { mutableStateOf(NumberBase.BINARY) }
    var n1 by remember { mutableStateOf("1011") }
    var n2 by remember { mutableStateOf("1101") }
    var isAdd by remember { mutableStateOf(true) }
    var activeIdx by remember { mutableStateOf(1) }

    val res = remember(n1, n2, base, isAdd) { NumberArithmetic.calculate(n1, n2, base, isAdd) }

    fun onKey(k: String) {
        val cur = if (activeIdx == 1) n1 else n2
        val next = when (k) {
            "AC" -> "0"
            "⌫" -> if (cur.length <= 1) "0" else cur.dropLast(1)
            "." -> if (!cur.contains(".")) "$cur." else cur
            "00" -> if (cur != "0") "${cur}00" else cur
            else -> if (cur == "0") k else "$cur$k"
        }
        if (activeIdx == 1) n1 = next else n2 = next
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NumberBase.values().forEach { b ->
                        val sel = b == base
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0xFF2563EB) else Color.Transparent)
                            .clickable { base = b; n1 = NumberConverter.sanitize(n1, b); n2 = NumberConverter.sanitize(n2, b) }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text(b.shortName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Color(0xFF475569))
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (activeIdx == 1) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)).clickable { activeIdx = 1 }.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সংখ্যা ১", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(n1, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFE2E8F0)).padding(2.dp)) {
                                Box(Modifier.size(32.dp).clip(CircleShape).background(if (isAdd) Color(0xFF2563EB) else Color.Transparent).clickable { isAdd = true }, contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isAdd) Color.White else Color(0xFF475569))
                                }
                                Box(Modifier.size(32.dp).clip(CircleShape).background(if (!isAdd) Color(0xFF2563EB) else Color.Transparent).clickable { isAdd = false }, contentAlignment = Alignment.Center) {
                                    Text("–", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (!isAdd) Color.White else Color(0xFF475569))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (activeIdx == 2) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)).clickable { activeIdx = 2 }.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("সংখ্যা ২", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(n2, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ফলাফল (${base.fullName})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                        Text("(${res.answer})_${base.radix}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                    Column(Modifier.padding(10.dp)) {
                        Text("ধাপভিত্তিক ICT সমাধান", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Spacer(Modifier.height(4.dp))
                        Text(res.explanation, fontSize = 12.sp, color = Color(0xFF1E293B), lineHeight = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        BaseKeypad(base, ::onKey)
    }
}

@Composable
fun TwosComplementTab() {
    var bin by remember { mutableStateOf("1011") }
    var bits by remember { mutableStateOf(8) }
    val res = remember(bin, bits) { TwosComplementCalculator.calculate(bin, bits) }

    fun onKey(k: String) {
        bin = when (k) {
            "AC" -> "0"
            "⌫" -> if (bin.length <= 1) "0" else bin.dropLast(1)
            "0" -> if (bin != "0") "${bin}0" else bin
            else -> if (bin == "0") "1" else "${bin}1"
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(4, 8, 16).forEach { b ->
                        val sel = b == bits
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) Color(0xFF2563EB) else Color.Transparent).clickable { bits = b }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text("$b Bits রেজিস্টার", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) Color.White else Color(0xFF475569))
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("প্রদত্ত বাইনারি মান", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(bin, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompItem("১. রেজিস্টার মান ($bits-বিট)", res.padded, "+${res.decimalMag}")
                        CompItem("২. ১-এর পরিপূরক (1's Comp)", res.onesComp, "0➔1, 1➔0")
                        CompItem("৩. ২-এর পরিপূরক (2's Comp)", res.twosComp, "১-এর পরিপূরক + ১", highlight = true)
                        CompItem("৪. সাইনযুক্ত ডেসিমেল মান", "${res.signedDecimal}", if (res.signedDecimal < 0) "ঋণাত্মক" else "ধনাত্মক", badgeRed = res.signedDecimal < 0)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                    Column(Modifier.padding(10.dp)) {
                        Text("ICT সমাধান প্রণালী", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                        Spacer(Modifier.height(4.dp))
                        Text(res.explanation, fontSize = 12.sp, color = Color(0xFF1E293B), lineHeight = 18.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Card(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFEE2E2)).clickable { onKey("AC") }, contentAlignment = Alignment.Center) {
                    Text("AC", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0)).clickable { onKey("⌫") }, contentAlignment = Alignment.Center) {
                    Text("⌫", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                }
                Box(Modifier.weight(1.2f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8FAFC)).clickable { onKey("0") }, contentAlignment = Alignment.Center) {
                    Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Box(Modifier.weight(1.2f).height(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF8FAFC)).clickable { onKey("1") }, contentAlignment = Alignment.Center) {
                    Text("1", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }
        }
    }
}

@Composable
fun CompItem(title: String, value: String, badge: String, highlight: Boolean = false, badgeRed: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B))
            Surface(shape = RoundedCornerShape(4.dp), color = (if (badgeRed) Color(0xFFDC2626) else Color(0xFF2563EB)).copy(alpha = 0.1f)) {
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (badgeRed) Color(0xFFDC2626) else Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (highlight) Color(0xFF2563EB) else Color(0xFF0F172A), fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun GridMenuIcon(modifier: Modifier = Modifier, tint: Color = Color(0xFF1E3A8A)) {
    Canvas(modifier.size(20.dp)) {
        val s = 8.dp.toPx(); val sp = 3.dp.toPx(); val r = 2.dp.toPx()
        drawRoundRect(tint, Offset(0f, 0f), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(s + sp, 0f), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(0f, s + sp), Size(s, s), CornerRadius(r, r))
        drawRoundRect(tint, Offset(s + sp, s + sp), Size(s, s), CornerRadius(r, r))
    }
}

class CalculatorParser(private val isDeg: Boolean = true) {
    private val funcs = setOf("sin", "cos", "tan", "log", "ln", "sqrt")

    fun evaluate(expr: String): Double {
        if (expr.isBlank()) return 0.0
        val s = expr.replace("×", "*").replace("÷", "/").replace("–", "-").replace("−", "-").replace("π", " ${Math.PI} ").replace("e", " ${Math.E} ")
        val raw = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c in "+-*/^()!%" -> { raw.add(c.toString()); i++ }
                c == '√' -> { raw.add("sqrt"); i++ }
                c.isDigit() || c == '.' -> { val st = i; while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++; raw.add(s.substring(st, i)) }
                c.isLetter() -> { val st = i; while (i < s.length && s[i].isLetter()) i++; raw.add(s.substring(st, i).lowercase()) }
                else -> i++
            }
        }
        val tokens = mutableListOf<String>()
        for (j in raw.indices) {
            tokens.add(raw[j])
            if (j + 1 < raw.size && raw[j] !in "+-*/^(" && raw[j] !in funcs && raw[j + 1] !in "+-*/^)!%") tokens.add("*")
        }
        return Evaluator(tokens, isDeg, funcs).parse()
    }

    private class Evaluator(private val tokens: List<String>, private val isDeg: Boolean, private val funcs: Set<String>) {
        private var pos = 0
        private fun peek() = if (pos < tokens.size) tokens[pos] else null
        private fun get() = tokens[pos++]
        fun parse() = parseExp()
        private fun parseExp(): Double {
            var v = parseTerm()
            while (peek() == "+" || peek() == "-") v = if (get() == "+") v + parseTerm() else v - parseTerm()
            return v
        }
        private fun parseTerm(): Double {
            var v = parsePow()
            while (peek() in listOf("*", "/", "%")) {
                val op = get(); val r = parsePow()
                v = when (op) { "*" -> v * r; "/" -> if (r == 0.0) throw ArithmeticException() else v / r; else -> v % r }
            }
            return v
        }
        private fun parsePow(): Double {
            var v = parseUnary()
            if (peek() == "^") { get(); v = v.pow(parsePow()) }
            return v
        }
        private fun parseUnary(): Double = when (peek()) {
            "+" -> { get(); parseUnary() }
            "-" -> { get(); -parseUnary() }
            else -> parsePost()
        }
        private fun parsePost(): Double {
            var v = parsePrim()
            while (peek() in listOf("!", "%")) {
                if (get() == "!") {
                    val n = v.roundToInt()
                    var f = 1.0; for (k in 2..n) f *= k; v = f
                } else v /= 100.0
            }
            return v
        }
        private fun parsePrim(): Double {
            val t = peek() ?: throw IllegalArgumentException()
            if (t == "(") { get(); val v = parseExp(); if (peek() == ")") get(); return v }
            if (t in funcs) {
                val fn = get(); val arg = if (peek() == "(") { get(); val v = parseExp(); if (peek() == ")") get(); v } else parseUnary()
                val a = if (isDeg) Math.toRadians(arg) else arg
                return when (fn) {
                    "sin" -> sin(a); "cos" -> cos(a); "tan" -> tan(a)
                    "log" -> log10(arg); "ln" -> ln(arg); "sqrt" -> sqrt(arg); else -> 0.0
                }
            }
            get()
            return t.toDoubleOrNull() ?: 0.0
        }
    }
}

fun formatDouble(v: Double): String {
    if (v.isNaN()) return "Error"
    val r = (v * 1e10).roundToLong() / 1e10
    return if (r == r.toLong().toDouble()) r.toLong().toString() else DecimalFormat("#.########").format(r)
}

data class ConvRes(val result: String, val intPart: String, val fracPart: String)
data class ExpModel(val shortSummary: String, val fullExplanation: String)

object NumberConverter {
    fun sanitize(s: String, b: NumberBase) = s.filter { b.isValid(it) || it == '.' }.ifEmpty { "0" }

    fun convert(input: String, from: NumberBase, to: NumberBase): ConvRes {
        if (input.isBlank() || from == to) return ConvRes(input.ifEmpty { "0" }, input, "")
        try {
            if (from == NumberBase.BINARY && to == NumberBase.OCTAL) return binToGroup(input, 3, 8)
            if (from == NumberBase.OCTAL && to == NumberBase.BINARY) return groupToBin(input, 3)
            if (from == NumberBase.BINARY && to == NumberBase.HEXADECIMAL) return binToGroup(input, 4, 16)
            if (from == NumberBase.HEXADECIMAL && to == NumberBase.BINARY) return groupToBin(input, 4)
            if (from == NumberBase.OCTAL && to == NumberBase.HEXADECIMAL) return binToGroup(groupToBin(input, 3).result, 4, 16)
            if (from == NumberBase.HEXADECIMAL && to == NumberBase.OCTAL) return binToGroup(groupToBin(input, 4).result, 3, 8)

            val p = input.split(".")
            val iS = p[0].ifEmpty { "0" }
            val fS = if (p.size > 1) p[1] else ""

            if (from == NumberBase.DECIMAL) {
                val iRes = BigInteger(iS).toString(to.radix).uppercase()
                var fRes = ""
                if (fS.isNotEmpty() && fS.toDoubleOrNull() != null) {
                    var f = "0.$fS".toDouble()
                    val sb = StringBuilder()
                    val digs = "0123456789ABCDEF"
                    for (step in 0 until 6) {
                        if (f == 0.0) break
                        val pr = f * to.radix
                        val d = pr.toInt()
                        sb.append(digs[d])
                        f = pr - d
                    }
                    fRes = sb.toString()
                }
                return ConvRes(if (fRes.isEmpty()) iRes else "$iRes.$fRes", iRes, fRes)
            } else if (to == NumberBase.DECIMAL) {
                val iRes = BigInteger(iS, from.radix).toString(10)
                var fRes = ""
                if (fS.isNotEmpty()) {
                    var dec = 0.0
                    val up = fS.uppercase()
                    for (idx in up.indices) dec += "0123456789ABCDEF".indexOf(up[idx]) * from.radix.toDouble().pow(-(idx + 1))
                    val fmt = DecimalFormat("#.######").format(dec)
                    fRes = if (fmt.startsWith("0.")) fmt.substring(2) else fmt
                }
                return ConvRes(if (fRes.isEmpty()) iRes else "$iRes.$fRes", iRes, fRes)
            }
        } catch (_: Exception) {}
        return ConvRes("0", "0", "")
    }

    private fun binToGroup(bin: String, gSize: Int, targetBase: Int): ConvRes {
        val p = bin.split(".")
        val iS = p[0].ifEmpty { "0" }
        val fS = if (p.size > 1) p[1] else ""
        val padI = (gSize - (iS.length % gSize)) % gSize
        val iRes = ("0".repeat(padI) + iS).chunked(gSize).joinToString("") { Integer.parseInt(it, 2).toString(targetBase).uppercase() }.trimStart('0').ifEmpty { "0" }
        var fRes = ""
        if (fS.isNotEmpty()) {
            val padF = (gSize - (fS.length % gSize)) % gSize
            fRes = (fS + "0".repeat(padF)).chunked(gSize).joinToString("") { Integer.parseInt(it, 2).toString(targetBase).uppercase() }
        }
        return ConvRes(if (fRes.isEmpty()) iRes else "$iRes.$fRes", iRes, fRes)
    }

    private fun groupToBin(s: String, gSize: Int): ConvRes {
        val p = s.split(".")
        val digs = "0123456789ABCDEF"
        val iRes = p[0].uppercase().map { digs.indexOf(it).toString(2).padStart(gSize, '0') }.joinToString("").trimStart('0').ifEmpty { "0" }
        val fRes = if (p.size > 1) p[1].uppercase().map { digs.indexOf(it).toString(2).padStart(gSize, '0') }.joinToString("") else ""
        return ConvRes(if (fRes.isEmpty()) iRes else "$iRes.$fRes", iRes, fRes)
    }

    fun generateExplanation(input: String, from: NumberBase, to: NumberBase, res: ConvRes): ExpModel {
        if (from == to) return ExpModel("উভয় পদ্ধতি একই (${from.bnName})।", "($input)_${from.radix} = ($input)_${to.radix}")
        val p = input.split(".")
        val iS = p[0].ifEmpty { "0" }
        val fS = if (p.size > 1) p[1] else ""
        val digs = "0123456789ABCDEF"

        if (from == NumberBase.DECIMAL) {
            val sb = StringBuilder("★ দশমিক থেকে ${to.bnName} রূপান্তর (ভিত্তি = ${to.radix})\n\n১. পূর্ণাংশের হিসাব ($iS):\n")
            var c = iS.toBigIntegerOrNull() ?: BigInteger.ZERO
            val baseB = to.radix.toBigInteger()
            if (c == BigInteger.ZERO) sb.append("• 0 ÷ ${to.radix} = 0, ভাগশেষ = 0\n")
            while (c > BigInteger.ZERO) {
                val rem = (c % baseB).toInt()
                val q = c / baseB
                sb.append("• $c ÷ ${to.radix} = $q, ভাগশেষ = ${digs[rem]}\n")
                c = q
            }
            sb.append("➔ নিচ থেকে উপরে সাজিয়ে: (${res.intPart})_${to.radix}\n\n")
            if (fS.isNotEmpty() && fS.toDoubleOrNull() != null) {
                sb.append("২. ভগ্নাংশের হিসাব (0.$fS):\n")
                var f = "0.$fS".toDouble()
                for (s in 1..6) {
                    if (f == 0.0) break
                    val pr = f * to.radix
                    val intP = pr.toInt()
                    sb.append("• ধাপ $s: ${String.format("%.4f", f)} × ${to.radix} = ${String.format("%.4f", pr)} ➔ পূর্ণাংশ: ${digs[intP]}\n")
                    f = pr - intP
                }
                sb.append("➔ উপর থেকে নিচে সাজিয়ে: (0.${res.fracPart})_${to.radix}\n\n")
            }
            sb.append("★ উত্তর: ($input)_10 = (${res.result})_${to.radix}")
            return ExpModel("পূর্ণাংশকে ${to.radix} দিয়ে পুনঃপুন ভাগ (নিচ থেকে উপরে) এবং ভগ্নাংশকে ${to.radix} দিয়ে গুণ (উপর থেকে নিচে)।", sb.toString())
        }

        if (to == NumberBase.DECIMAL) {
            val sb = StringBuilder("★ ${from.bnName} থেকে দশমিকে রূপান্তর (ভিত্তি = ${from.radix})\n\n১. স্থানীয় মানের ঘাত বিস্তার:\n")
            val up = iS.uppercase()
            val terms = up.indices.map { idx -> "${up[idx]} × ${from.radix}^${up.length - 1 - idx}" }
            sb.append("= " + terms.joinToString(" + ") + " = ${res.intPart}\n\n")
            if (fS.isNotEmpty()) {
                val fUp = fS.uppercase()
                val fTerms = fUp.indices.map { idx -> "${fUp[idx]} × ${from.radix}^(-${idx + 1})" }
                sb.append("২. ভগ্নাংশের বিস্তার:\n= " + fTerms.joinToString(" + ") + " = 0.${res.fracPart}\n\n")
            }
            sb.append("★ উত্তর: ($input)_${from.radix} = (${res.result})_10")
            return ExpModel("প্রতিটি অঙ্ককে ভিত্তির ঘাত (${from.radix}^n) দিয়ে গুণ করে যোগফল নির্ণয়।", sb.toString())
        }

        if (from == NumberBase.BINARY && (to == NumberBase.OCTAL || to == NumberBase.HEXADECIMAL)) {
            val g = if (to == NumberBase.OCTAL) 3 else 4
            return ExpModel("বাইনারি বিটগুলোকে $g-টি করে গ্রুপিং করে সমতুল্য ${to.bnName} মান বসানো।",
                "★ বাইনারি থেকে ${to.bnName} ($g-বিট গ্রুপিং):\nপূর্ণাংশকে ডান থেকে বামে এবং ভগ্নাংশকে বাম থেকে ডানে $g-টি করে বিট গ্রুপ করা হয়েছে।\n★ উত্তর: ($input)_2 = (${res.result})_${to.radix}")
        }

        if ((from == NumberBase.OCTAL || from == NumberBase.HEXADECIMAL) && to == NumberBase.BINARY) {
            val g = if (from == NumberBase.OCTAL) 3 else 4
            return ExpModel("প্রতিটি ${from.bnName} অঙ্ককে $g-বিট বাইনারিতে রূপান্তর।",
                "★ ${from.bnName} থেকে বাইনারি:\nপ্রতিটি অঙ্ককে তার সমতুল্য $g-বিট বাইনারি মানে প্রকাশ করা হয়েছে।\n★ উত্তর: ($input)_${from.radix} = (${res.result})_2")
        }

        return ExpModel("${from.bnName} থেকে বাইনারি হয়ে ${to.bnName} রূপান্তর (মাধ্যম রূপান্তর)।",
            "★ বাইনারি মাধ্যম রূপান্তর:\n১. ($input)_${from.radix} ➔ বাইনারিতে রূপান্তর\n২. বাইনারি মানকে কাঙ্ক্ষিত পদ্ধতিতে গ্রুপিং ➔ (${res.result})_${to.radix}")
    }
}

data class ArithRes(val answer: String, val explanation: String)
object NumberArithmetic {
    fun calculate(n1: String, n2: String, b: NumberBase, isAdd: Boolean): ArithRes {
        try {
            val v1 = BigInteger(n1.filter { b.isValid(it) }.ifEmpty { "0" }, b.radix)
            val v2 = BigInteger(n2.filter { b.isValid(it) }.ifEmpty { "0" }, b.radix)
            return if (isAdd) {
                val ans = (v1 + v2).toString(b.radix).uppercase()
                ArithRes(ans, "★ কলামভিত্তিক যোগ (ভিত্তি = ${b.radix}):\nকলামের যোগফল ভিত্তির সমান বা বড় হলে ভিত্তি বিয়োগ করে ফলাফল বসে এবং ১ ক্যারি পরবর্তী কলামে যায়।\nযোগফল = ($ans)_${b.radix}")
            } else {
                val diff = v1 - v2
                val sign = if (diff < BigInteger.ZERO) "-" else ""
                val ans = sign + diff.abs().toString(b.radix).uppercase()
                ArithRes(ans, "★ কলামভিত্তিক বিয়োগ (ভিত্তি = ${b.radix}):\nউপরের অঙ্ক ছোট হলে ভিত্তি (${b.radix}) ধার নিয়ে বিয়োগ করতে হয় এবং পরবর্তী কলামের জন্য ধার ১ থাকে।\nবিয়োগফল = ($ans)_${b.radix}")
            }
        } catch (_: Exception) { return ArithRes("Error", "ত্রুটি: সঠিক সংখ্যা দিন।") }
    }
}

data class TwosRes(val padded: String, val onesComp: String, val twosComp: String, val decimalMag: Long, val signedDecimal: Long, val explanation: String)
object TwosComplementCalculator {
    fun calculate(bin: String, bits: Int): TwosRes {
        val cln = bin.filter { it in "01" }.trimStart('0').ifEmpty { "0" }
        val mag = try { cln.toLong(2) } catch (_: Exception) { 0L }
        val pad = cln.padStart(bits, '0')
        val ones = pad.map { if (it == '0') '1' else '0' }.joinToString("")
        var c = 1
        val twosA = CharArray(bits)
        for (i in bits - 1 downTo 0) {
            val s = ones[i].digitToInt() + c
            twosA[i] = if (s % 2 == 1) '1' else '0'
            c = s / 2
        }
        val twos = String(twosA)
        val exp = "★ $bits-বিট রেজিস্টারে ২-এর পরিপূরক গঠন:\nধাপ ১ ($bits-বিটে সাজানো): $pad (মান: +$mag)\nধাপ ২ (১-এর পরিপূরক বিট উল্টিয়ে): $ones\nধাপ ৩ (১ যোগ করে ২-এর পরিপূরক): $twos\nধাপ ৪ (চিহ্ন বিট analysis): সর্ববাম বিট '${twos.first()}' হওয়ায় সংখ্যাটি ঋণাত্মক।\nডেসিমেল মান = -$mag"
        return TwosRes(pad, ones, twos, mag, -mag, exp)
    }
}
