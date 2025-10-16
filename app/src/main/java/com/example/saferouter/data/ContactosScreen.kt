package com.example.saferouter.data

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.saferouter.R
import com.example.saferouter.presentation.model.Contact
import com.example.saferouter.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ContactosScreen(
    db: FirebaseFirestore,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val contacts = remember { mutableStateOf<List<Contact>>(emptyList()) }
    val selectedContacts = remember { mutableStateOf<Set<Contact>>(emptySet()) }
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val coroutineScope = rememberCoroutineScope()


    // Permiso READ_CONTACTS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            contacts.value = getAllPhoneContacts(context)
        } else {
            // Mostrar explicación de por qué se necesita el permiso
            Toast.makeText(context, "El permiso de contactos es necesario para agregar contactos de emergencia", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasContactPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contacts.value = getAllPhoneContacts(context)
        }
    }

    // Cargar contactos ya guardados en Firestore y marcar como seleccionados
    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                val snapshot = db.collection("users").document(uid).get().await()
                val savedList = (snapshot.get("contacts") as? List<*>)?.mapNotNull {
                    val map = it as? Map<*, *>
                    val nombre = map?.get("nombre") as? String
                    val telefono = map?.get("telefono") as? String
                    if (nombre != null && telefono != null) Contact(nombre, telefono) else null
                } ?: emptyList()
                selectedContacts.value = savedList.toSet()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar contactos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundWhite)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back_24),
                    contentDescription = "Back",
                    tint = PrimaryBlueDark
                )
            }
            Text(
                "Contactos de Emergencia",
                color = PrimaryBlueDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {

            // Lista de contactos con selección múltiple
            Text(
                "Tus Contactos de emergencia",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn {
                items(contacts.value) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = selectedContacts.value.contains(contact),
                        onSelect = {
                            selectedContacts.value = if (selectedContacts.value.contains(contact)) {
                                selectedContacts.value - contact
                            } else {
                                selectedContacts.value + contact
                            }
                        },
                        onDelete = {
                            // Eliminar contacto de Firestore y de la lista seleccionada
                            if (uid != null) {
                                coroutineScope.launch {
                                    try {
                                        db.collection("users").document(uid)
                                            .update(
                                                "contacts",
                                                FieldValue.arrayRemove(
                                                    mapOf("nombre" to contact.nombre, "telefono" to contact.telefono)
                                                )
                                            ).await()
                                        selectedContacts.value = selectedContacts.value - contact
                                        contacts.value = contacts.value - contact
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error eliminando: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guardar contactos seleccionados en Firestore
            Button(
                onClick = {
                    if (uid != null) {
                        coroutineScope.launch {
                            try {
                                val userRef = db.collection("users").document(uid)

                                // Obtener contactos actuales desde Firestore
                                val snapshot = userRef.get().await()
                                val currentContacts = (snapshot.get("contacts") as? List<*>)?.mapNotNull {
                                    val map = it as? Map<*, *>
                                    val nombre = map?.get("nombre") as? String
                                    val telefono = map?.get("telefono") as? String
                                    if (nombre != null && telefono != null) Contact(nombre, telefono) else null
                                } ?: emptyList()

                                // Calcular los que fueron desmarcados
                                val removedContacts = currentContacts.filter { it !in selectedContacts.value }

                                // Primero eliminar los que ya no están seleccionados
                                for (contact in removedContacts) {
                                    userRef.update(
                                        "contacts",
                                        FieldValue.arrayRemove(
                                            mapOf("nombre" to contact.nombre, "telefono" to contact.telefono)
                                        )
                                    ).await()
                                }

                                // Luego agregar los nuevos seleccionados
                                userRef.update(
                                    "contacts",
                                    FieldValue.arrayUnion(*selectedContacts.value.map {
                                        mapOf("nombre" to it.nombre, "telefono" to it.telefono)
                                    }.toTypedArray())
                                ).await()

                                Toast.makeText(context, "✅ Contactos actualizados", Toast.LENGTH_SHORT).show()
                                navigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlueDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar seleccionados", color = Color.White, fontWeight = FontWeight.Bold)
            }

        }
    }
}



fun sendInviteSms(context: Context, contact: Contact) {
    try {
        val phoneUri = Uri.parse("smsto:${contact.telefono}")
        val intent = Intent(Intent.ACTION_SENDTO, phoneUri)
        val message = """
            ¡Hola ${contact.nombre}! 😊 
            Te invito a probar SafeRouter, una app que me permite compartir mi ubicación y avisarte en caso de emergencia. 
            Puedes descargarla o contactarme para saber más 🚀
        """.trimIndent()
        intent.putExtra("sms_body", message)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir la app de mensajes", Toast.LENGTH_SHORT).show()
    }
}


// Leer todos los contactos del teléfono
fun getAllPhoneContacts(context: Context): List<Contact> {
    val contactsList = mutableListOf<Contact>()
    val cursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )
    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            contactsList.add(Contact(name, phone))
        }
    }
    return contactsList
}




@Composable
fun ContactItem(
    contact: Contact,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (isSelected) PrimaryBlueLight else BackgroundWhite
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(contact.nombre, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(contact.telefono, color = TextSecondary, fontSize = 14.sp)
            }
            Row {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) R.drawable.check else R.drawable.check),
                        contentDescription = "Seleccionar",
                        tint = if (isSelected) PrimaryBlueDark else AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Eliminar",
                        tint = AlertRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = { sendInviteSms(context, contact) },
                    modifier = Modifier.size(32.dp).padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notifications), // un ícono tipo mensaje
                        contentDescription = "Invitar",
                        tint = PrimaryBlueDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

            }
        }
    }
}
