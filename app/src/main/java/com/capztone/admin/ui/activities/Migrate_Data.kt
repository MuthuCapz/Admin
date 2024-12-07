package com.capztone.admin.ui.activities


import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.R
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class Migrate_Data : AppCompatActivity() {

    private lateinit var etPath: EditText
    private lateinit var btnMigrate: Button
    private lateinit var tvStatus: TextView

    // Test and Production Firebase References
    private lateinit var testDatabaseRef: DatabaseReference
    private lateinit var prodDatabaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_migrate_data)

        // Initialize the views
        etPath = findViewById(R.id.etPath)
        btnMigrate = findViewById(R.id.btnMigrate)
        tvStatus = findViewById(R.id.tvStatus)

        // Set Firebase References for Test and Production
        testDatabaseRef = FirebaseDatabase.getInstance("https://fishfyproduction.firebaseio.com/").reference
        prodDatabaseRef = FirebaseDatabase.getInstance("https://sea1-2a585-default-rtdb.firebaseio.com/").reference

        // Migrate Data Button
        btnMigrate.setOnClickListener {
            val paths = etPath.text.toString().trim()

            if (paths.isNotEmpty()) {
                // Split paths by comma and trim any extra spaces
                val pathList = paths.split(",").map { it.trim() }
                migrateData(pathList)
            } else {
                Toast.makeText(this, "Please enter paths separated by commas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to migrate data for a list of paths
    private fun migrateData(paths: List<String>) {
        var migrationCount = 0

        paths.forEach { path ->
            if (path.isNotEmpty()) {
                // Reference to the path in Test environment
                val testRef = testDatabaseRef.child(path)

                // Retrieve data from the Test environment
                testRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val data = snapshot.value

                            // Reference to the same path in Production
                            val prodRef = prodDatabaseRef.child(path)

                            // Push data to Production
                            prodRef.setValue(data).addOnSuccessListener {
                                migrationCount++
                                tvStatus.text = "Successfully migrated $migrationCount of ${paths.size} paths."
                                if (migrationCount == paths.size) {
                                    Toast.makeText(this@Migrate_Data, "All migrations successful!", Toast.LENGTH_SHORT).show()
                                }
                            }.addOnFailureListener {
                                tvStatus.text = "Failed to migrate data for path: $path"
                                Toast.makeText(this@Migrate_Data, "Migration failed for path: $path", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            tvStatus.text = "No data found for path: $path"
                            Toast.makeText(this@Migrate_Data, "No data found at $path", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        tvStatus.text = "Database error for path: $path - ${error.message}"
                        Toast.makeText(this@Migrate_Data, "Failed to fetch data for $path", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}
