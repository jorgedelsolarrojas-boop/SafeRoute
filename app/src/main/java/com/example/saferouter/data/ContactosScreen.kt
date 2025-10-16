package com.example.saferouter.data

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
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
            Log.d("ContactosScreen", "Permiso concedido. Contactos cargados: ${contacts.value.size}")
        } else {
            Log.w("ContactosScreen", "Permiso de contactos denegado")
            Toast.makeText(context, "El permiso de contactos es necesario para agregar contactos de emergencia", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasContactPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) {
            Log.d("ContactosScreen", "Solicitando permiso de contactos")
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            contacts.value = getAllPhoneContacts(context)
            Log.d("ContactosScreen", "Contactos del teléfono cargados: ${contacts.value.size}")
        }
    }

    // Cargar contactos ya guardados en Firestore y marcar como seleccionados
    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                Log.d("ContactosScreen", "Cargando contactos guardados para UID: $uid")
                val snapshot = db.collection("users").document(uid).get().await()
                val savedList = (snapshot.get("contacts") as? List<*>)?.mapNotNull {
                    val map = it as? Map<*, *>
                    val nombre = map?.get("nombre") as? String
                    val telefono = map?.get("telefono") as? String
                    if (nombre != null && telefono != null) Contact(nombre, telefono) else null
                } ?: emptyList()

                Log.d("ContactosScreen", "Contactos cargados desde Firestore: ${savedList.size}")
                savedList.forEach { contact ->
                    Log.d("ContactosScreen", " - ${contact.nombre}: ${contact.telefono}")
                }

                selectedContacts.value = savedList.toSet()
            } catch (e: Exception) {
                Log.e("ContactosScreen", "Error al cargar contactos: ${e.message}", e)
                Toast.makeText(context, "Error al cargar contactos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("ContactosScreen", "UID es null, no se pueden cargar contactos")
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

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(contacts.value) { contact ->
                    ContactItem(
                        contact = contact,
                        isSelected = selectedContacts.value.contains(contact),
                        onSelect = {
                            val newSelection = if (selectedContacts.value.contains(contact)) {
                                Log.d("ContactosScreen", "Deseleccionando: ${contact.nombre}")
                                selectedContacts.value - contact
                            } else {
                                Log.d("ContactosScreen", "Seleccionando: ${contact.nombre}")
                                selectedContacts.value + contact
                            }
                            selectedContacts.value = newSelection
                        },
                        onDelete = {
                            // Eliminar contacto de Firestore y de la lista seleccionada
                            if (uid != null) {
                                coroutineScope.launch {
                                    try {
                                        Log.d("ContactosScreen", "Eliminando contacto: ${contact.nombre}")
                                        db.collection("users").document(uid)
                                            .update(
                                                "contacts",
                                                FieldValue.arrayRemove(
                                                    mapOf("nombre" to contact.nombre, "telefono" to contact.telefono)
                                                )
                                            ).await()
                                        selectedContacts.value = selectedContacts.value - contact
                                        Log.d("ContactosScreen", "✅ Contacto eliminado exitosamente")
                                        Toast.makeText(context, "Contacto eliminado", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("ContactosScreen", "❌ Error eliminando: ${e.message}", e)
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
                                Log.d("ContactosScreen", "Iniciando guardado de ${selectedContacts.value.size} contactos")

                                val userRef = db.collection("users").document(uid)

                                // Convertir a formato para Firestore
                                val contactsToSave = selectedContacts.value.map {
                                    Log.d("ContactosScreen", "Preparando para guardar: ${it.nombre} - ${it.telefono}")
                                    mapOf("nombre" to it.nombre, "telefono" to it.telefono)
                                }

                                // Usar set con merge para asegurar que se actualice correctamente
                                userRef.set(
                                    mapOf("contacts" to contactsToSave),
                                    com.google.firebase.firestore.SetOptions.merge()
                                ).await()

                                Log.d("ContactosScreen", "✅ Contactos guardados exitosamente en Firestore")

                                // Verificar que se guardaron correctamente
                                val verification = userRef.get().await()
                                val verifyList = verification.get("contacts") as? List<*>
                                Log.d("ContactosScreen", "Verificación: ${verifyList?.size ?: 0} contactos en Firestore")

                                Toast.makeText(context, "✅ Contactos actualizados", Toast.LENGTH_SHORT).show()
                                navigateBack()
                            } catch (e: Exception) {
                                Log.e("ContactosScreen", "❌ Error guardando contactos: ${e.message}", e)
                                Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.w("ContactosScreen", "No se puede guardar: UID es null")
                        Toast.makeText(context, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryBlueDark),
                shape = RoundedCornerShape(12.dp),
                enabled = selectedContacts.value.isNotEmpty()
            ) {
                Text(
                    "Guardar seleccionados (${selectedContacts.value.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun sendInviteSms(context: Context, contact: Contact) {
    try {
        Log.d("ContactosScreen", "Enviando invitación a: ${contact.nombre}")
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
        Log.e("ContactosScreen", "Error enviando SMS: ${e.message}", e)
        Toast.makeText(context, "No se pudo abrir la app de mensajes", Toast.LENGTH_SHORT).show()
    }
}

// Leer todos los contactos del teléfono
fun getAllPhoneContacts(context: Context): List<Contact> {
    val contactsList = mutableListOf<Contact>()
    try {
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
        Log.d("ContactosScreen", "Total de contactos leídos del teléfono: ${contactsList.size}")
    } catch (e: Exception) {
        Log.e("ContactosScreen", "Error leyendo contactos del teléfono: ${e.message}", e)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.nombre, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(contact.telefono, color = TextSecondary, fontSize = 14.sp)
            }
            Row {
                IconButton(
                    onClick = onSelect,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.check),
                        contentDescription = if (isSelected) "Deseleccionar" else "Seleccionar",
                        tint = if (isSelected) PrimaryBlueDark else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isSelected) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = "Eliminar",
                            tint = AlertRed,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { sendInviteSms(context, contact) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notifications),
                            contentDescription = "Invitar",
                            tint = PrimaryBlueDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}