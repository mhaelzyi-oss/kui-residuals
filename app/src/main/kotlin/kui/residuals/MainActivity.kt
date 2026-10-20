package kui.residuals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private data class Leftover(
    val name: String,
    val label: String,
    val places: List<String>,
    val hidden: Boolean,
    val notes: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val cream = Color(0xFFF7F4EE)
            val gold = Color(0xFFC9A227)
            val caution = Color(0xFF6B1414)
            val cautionGold = Color(0xFFE8C547)
            MaterialTheme(colorScheme = lightColorScheme(primary = gold, background = cream, surface = cream)) {
                var terms by rememberSaveable { mutableStateOf(true) }
                var tab by rememberSaveable { mutableStateOf(0) }
                var leftover by rememberSaveable {
                    mutableStateOf(
                        Leftover(
                            name = "com.example.old",
                            label = "Old Example",
                            places = listOf("The app's own files", "Hidden leftover", "Extra game files"),
                            hidden = true,
                            notes = "Old settings the app left behind.",
                        ),
                    )
                }
                var held by rememberSaveable { mutableStateOf(false) }
                var gone by rememberSaveable { mutableStateOf(false) }
                var copied by rememberSaveable { mutableStateOf(false) }
                var confirm by rememberSaveable { mutableStateOf(false) }
                var editing by rememberSaveable { mutableStateOf(false) }
                var notes by rememberSaveable { mutableStateOf("Old settings the app left behind.") }
                var status by rememberSaveable { mutableStateOf("This phone, ready") }
                if (terms) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("KUI Residuals", color = gold, style = MaterialTheme.typography.labelLarge)
                            Text("Before you start", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                            Text(stringResource(R.string.terms), modifier = Modifier.padding(top = 16.dp))
                        }
                        Button(onClick = { terms = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("I own this phone")
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.weight(1f).padding(24.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("KUI Residuals", color = gold, style = MaterialTheme.typography.labelLarge)
                                Text(status, color = Color(0xFF6E6E73), style = MaterialTheme.typography.bodySmall)
                            }
                            when (tab) {
                                0 -> {
                                    Text("Leftovers", style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        if (gone) "No leftover app folders."
                                        else "One uninstalled app left folders behind, including hidden ones.",
                                        color = Color(0xFF6E6E73),
                                    )
                                    if (!gone && !held) {
                                        Text(leftover.label, style = MaterialTheme.typography.titleLarge)
                                        Text(leftover.name, color = Color(0xFF6E6E73))
                                        leftover.places.forEach { Text("· $it") }
                                        Button(onClick = {
                                            held = true
                                            status = "Set aside"
                                        }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Set aside")
                                        }
                                        Button(
                                            onClick = {
                                                copied = true
                                                status = "Copy saved on this phone"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = caution, contentColor = cautionGold),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text("Save a copy")
                                        }
                                        TextButton(onClick = { editing = true }) { Text("Open files and edit") }
                                    } else if (held && !gone) {
                                        Text("Set aside", color = gold, style = MaterialTheme.typography.labelLarge)
                                        Text(leftover.label, style = MaterialTheme.typography.titleLarge)
                                        Text("Waiting. Delete forever only works here.")
                                        Button(
                                            onClick = {
                                                copied = true
                                                status = "Copy saved"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = caution, contentColor = cautionGold),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text("Save a copy")
                                        }
                                        Button(
                                            onClick = {
                                                if (!confirm) {
                                                    confirm = true
                                                    status = "Confirm delete"
                                                } else {
                                                    gone = true
                                                    held = false
                                                    status = "Deleted"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = caution, contentColor = cautionGold),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(if (confirm) "Confirm delete" else "Delete forever")
                                        }
                                    }
                                    if (copied) Text("A copy is on this phone. Nothing was uploaded.")
                                    if (editing && !gone) {
                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            label = { Text("notes.txt") },
                                        )
                                        Button(onClick = {
                                            leftover = leftover.copy(notes = notes)
                                            editing = false
                                            status = "Saved on this phone"
                                        }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Save edits")
                                        }
                                    }
                                }
                                1 -> {
                                    Text("Files", style = MaterialTheme.typography.headlineMedium)
                                    Text("The app's own files, extra game files, and photos from apps. Hidden leftover names are included.")
                                    if (!gone && !held) {
                                        Button(onClick = { tab = 0 }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Open Old Example leftover")
                                        }
                                    } else {
                                        Text("Leftover folders are set aside or gone.")
                                    }
                                    Text("Still on the phone", color = Color(0xFF6E6E73))
                                    Text("Live Example — still installed. We leave it.")
                                }
                                else -> {
                                    Text("Debug", style = MaterialTheme.typography.headlineMedium)
                                    Text("Crashes and freezes on this phone. Copy a note. KUI Desk can open it on the computer.")
                                    Button(onClick = { status = "Copied. The note stayed on this machine." }, modifier = Modifier.fillMaxWidth()) {
                                        Text("Copy a short note")
                                    }
                                }
                            }
                        }
                        NavigationBar {
                            listOf("Leftovers", "Files", "Debug").forEachIndexed { i, label ->
                                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(label) })
                            }
                        }
                    }
                }
            }
        }
    }
}
