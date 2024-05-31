package com.example.patrimapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import java.io.Serializable

data class Place(
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
): Serializable

class placeMain : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_main)
        getLocation()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //instancia varias ubicacion
        // porovedores
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obteniendo los valores de la bace de datos
        val placeRef = db.collection("place")
        val placeContainer = findViewById<LinearLayout>(R.id.productoContainer)
        placeRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val place = document.toObject(Place::class.java)
                    addPlaceContainer(place, placeContainer, document.id)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener lugares", exception)
                Toast.makeText(this, "Error al obtener lugares", Toast.LENGTH_SHORT).show()
            }


        // Logica agregar lugar
        val btArrow = findViewById<ImageView>(R.id.cartIcon)
        btArrow.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                getLocation1()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    123
                )
            }

        }

        val verRegistrosButton = findViewById<ImageView>(R.id.registros)
        verRegistrosButton.setOnClickListener {
            showRecordsCountDialog()
        }




    }


    private fun addPlaceContainer(place: Place, contenedor: LinearLayout, lugarId: String) {
        val inflater = layoutInflater
        val placeView = inflater.inflate(R.layout.activity_place, contenedor, false)
        val namePlace = placeView.findViewById<TextView>(R.id.placeNameTextView)
        val lat = placeView.findViewById<TextView>(R.id.latitudeTextView)
        val lon = placeView.findViewById<TextView>(R.id.longitudeTextView)

        namePlace.text = place.nombre
        lat.text = place.latitud.toString()
        lon.text = place.longitud.toString()
        placeView.tag = lugarId
        contenedor.addView(placeView)

        val deleteButton = placeView.findViewById<ImageView>(R.id.deleteIcon)
        deleteButton.setOnClickListener {
            deletePlace(lugarId)
            contenedor.removeView(placeView)
        }

        val editarButton = placeView.findViewById<ImageView>(R.id.editIcon)
        editarButton.setOnClickListener {
            showDialogEdit(lugarId)
        }

        val signButton = findViewById<ImageView>(R.id.cerrar_sesion)
        signButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Cerrando Sesion", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, login::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun deletePlace(lugarId: String) {
        val lugarRef = db.collection("place").document(lugarId)
        lugarRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Lugar eliminado exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al eliminar el lugar", e)
                Toast.makeText(this, "Error al eliminar el lugar", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation1() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                printLocation(location)
                uiAddPlace(location)
            } else {
                Toast.makeText(this, "No se puede obtener la ubicación", Toast.LENGTH_SHORT).show()
            }
        }

        val locationRequest = LocationRequest.create()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {

                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun printLocation(location: Location) {
        Log.d("GPS", "LAT: ${location.latitude} - LON: ${location.longitude}")
    }

    private fun getLocation() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                Toast.makeText(this@placeMain, "Permiso otorgado", Toast.LENGTH_SHORT).show()
            }

            override fun onPermissionDenied(deniedPermissions: List<String>) {
                Toast.makeText(
                    this@placeMain,
                    "Permiso denegado\n$deniedPermissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        TedPermission.create()
            .setPermissionListener(permissionlistener)
            .setDeniedMessage("Si rechaza el permiso, no podrá utilizar este servicio.\n\nActive los permisos en[Setting] > [Permisos]")
            .setPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .check()
    }
        // añadir lugar
    private fun uiAddPlace(location: Location) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ingrese el nombre del lugar")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val namePlace = input.text.toString()
            if (namePlace.isNotBlank()) {
                addPlace(namePlace, location)
            } else {
                Toast.makeText(this, "Por favor ingrese un nombre válido", Toast.LENGTH_SHORT)
                    .show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun addPlace(name: String, location: Location) {
        val place = Place(name, location.latitude, location.longitude)
        db.collection("place")
            .add(place)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Lugar agregado correctamente", Toast.LENGTH_SHORT).show()
                addPlaceContainer(place, findViewById(R.id.productoContainer), documentReference.id)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al agregar lugar", e)
                Toast.makeText(this, "Error al agregar lugar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDialogEdit(id_lugar: String) {
        val options = arrayOf("Editar nombre", "Editar ubicación")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Qué desea editar?")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showUiNamePlace(id_lugar)
                1 -> showUiUdpateLocation(id_lugar)
            }
        }
        builder.show()
    }

    private fun showUiNamePlace(id_lugar: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar nombre del lugar")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val updateName = input.text.toString()


            if (updateName.isNotBlank()) {
                val placeRef = db.collection("place").document(id_lugar)


                placeRef.update("nombre", updateName)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Nombre actualizado correctamente", Toast.LENGTH_SHORT)
                            .show()

                        // Actualizar la UI
                        val placeContainer = findViewById<LinearLayout>(R.id.productoContainer)
                        val placeView = placeContainer.findViewWithTag<LinearLayout>(id_lugar)
                        if (placeView != null) {
                            val namePlace = placeView.findViewById<TextView>(R.id.placeNameTextView)
                            namePlace.text = updateName
                        }

                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar el nombre", Toast.LENGTH_SHORT)
                            .show()
                        Log.e("Firebase", "Error actualizando el nombre", e)
                    }
            } else {
                Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun showUiUdpateLocation(id_lugar: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Actualizar Ubicación del Lugar")

        builder.setMessage("Se utilizará la ubicación actual del dispositivo para actualizar la ubicación del lugar.")

        builder.setPositiveButton("Guardar") { dialog, _ ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val updateLat= location.latitude
                    val updateLog = location.longitude

                    val lugarRef = db.collection("place").document(id_lugar)
                    lugarRef.update("latitud", updateLat, "longitud", updateLog)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Ubicación actualizada correctamente", Toast.LENGTH_SHORT).show()

                            // Actualizar la UI
                            val placeContainer = findViewById<LinearLayout>(R.id.productoContainer)
                            val placeView = placeContainer.findViewWithTag<LinearLayout>(id_lugar)
                            if (placeView != null) {
                                val lat = placeView.findViewById<TextView>(R.id.latitudeTextView)
                                val lon = placeView.findViewById<TextView>(R.id.longitudeTextView)
                                lat.text = updateLat.toString()
                                lon.text = updateLog .toString()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al actualizar la ubicación", Toast.LENGTH_SHORT).show()
                            Log.e("Firebase", "Error actualizando la ubicación", e)
                        }
                } else {
                    Toast.makeText(this, "No se puede obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    private fun showRecordsCount(count: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cantidad de Registros")
        builder.setMessage("La cantidad de registros capturados es: $count")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    // Método para mostrar la cantidad de registros capturados en una ventana emergente al hacer clic en el botón "Ver Registros"
    private fun showRecordsCountDialog() {
        db.collection("place")
            .get()
            .addOnSuccessListener { documents ->
                showRecordsCount(documents.size())
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error al obtener la cantidad de registros", exception)
                Toast.makeText(this, "Error al obtener la cantidad de registros", Toast.LENGTH_SHORT).show()
            }
    }



}