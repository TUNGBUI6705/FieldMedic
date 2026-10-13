package com.example.fieldmedic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.fieldmedic.BuildConfig
import com.example.fieldmedic.data.local.crypto.DbKeyManager
import com.example.fieldmedic.data.local.db.DatabaseHolder
import com.example.fieldmedic.ui.theme.FieldMedicTheme
import com.example.fieldmedic.util.DebugSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// LƯU Ý: Màn hình này là màn hình debug tối giản để kiểm tra thiết lập khóa và mở khóa CSDL bằng BiometricPrompt.
// Sẽ được thay thế bằng màn hình Login/Lock thật trong các phần tiếp theo.
class MainActivity : FragmentActivity() {

    private lateinit var dbKeyManager: DbKeyManager
    private lateinit var databaseHolder: DatabaseHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dbKeyManager = DbKeyManager(applicationContext)
        databaseHolder = DatabaseHolder(applicationContext)

        setContent {
            FieldMedicTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DebugCryptoScreen(
                        activity = this,
                        dbKeyManager = dbKeyManager,
                        databaseHolder = databaseHolder,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DebugCryptoScreen(
    activity: FragmentActivity,
    dbKeyManager: DbKeyManager,
    databaseHolder: DatabaseHolder,
    modifier: Modifier = Modifier
) {
    var isInitialized by remember { mutableStateOf(dbKeyManager.isInitialized()) }
    var dbStatus by remember { mutableStateOf("Chưa mở") }
    var patientCount by remember { mutableStateOf(-1) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSeeding by remember { mutableStateOf(false) }

    val scope = remember { CoroutineScope(Dispatchers.Main) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Trạng thái DB: $dbStatus")
        if (patientCount >= 0) {
            Text(text = "Số lượng bệnh nhân: $patientCount")
        }
        if (errorMessage != null) {
            Text(text = "Lỗi: $errorMessage", color = MaterialTheme.colorScheme.error)
        }
        if (isSeeding) {
            Text(text = "Đang tạo dữ liệu thử...", color = MaterialTheme.colorScheme.primary)
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

        if (!isInitialized) {
            Button(onClick = {
                scope.launch {
                    try {
                        errorMessage = null
                        val rawKey = dbKeyManager.setup(activity)
                        databaseHolder.open(rawKey)
                        rawKey.fill(0) // Xóa mảng khóa ngay sau khi truyền cho DatabaseHolder.open()

                        dbStatus = "DB mở"
                        isInitialized = true
                        try {
                            patientCount = databaseHolder.requireDb().patientDao().findDuplicates("", 0, null).size
                        } catch (_: Exception) {
                            patientCount = 0
                        }
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: e.toString()
                    }
                }
            }) {
                Text(text = "Thiết lập")
            }
        } else {
            Button(onClick = {
                scope.launch {
                    try {
                        errorMessage = null
                        val rawKey = dbKeyManager.unlock(activity)
                        databaseHolder.open(rawKey)
                        rawKey.fill(0) // Xóa mảng khóa ngay sau khi truyền cho DatabaseHolder.open()

                        dbStatus = "DB mở"
                        try {
                            patientCount = databaseHolder.requireDb().patientDao().findDuplicates("", 0, null).size
                        } catch (_: Exception) {
                            patientCount = 0
                        }
                    } catch (e: Exception) {
                        errorMessage = e.localizedMessage ?: e.toString()
                    }
                }
            }) {
                Text(text = "Mở khóa")
            }
        }

        // Nút "Tạo dữ liệu thử" chỉ hiện trong bản debug khi DB đã mở
        if (BuildConfig.DEBUG && dbStatus == "DB mở") {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Button(
                enabled = !isSeeding,
                onClick = {
                    scope.launch {
                        try {
                            isSeeding = true
                            errorMessage = null
                            DebugSeeder.seed(databaseHolder.requireDb(), patientsCount = 10_000, encountersPerPatient = 5)
                            patientCount = databaseHolder.requireDb().patientDao().findDuplicates("", 0, null).size
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: e.toString()
                        } finally {
                            isSeeding = false
                        }
                    }
                }
            ) {
                Text(text = "Tạo dữ liệu thử (10k)")
            }
        }
    }
}
