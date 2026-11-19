<Без темы>
Александр Меньшиков
Сегодня, 14:14
Кому:вам
package com.example.loyaltycards
 
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
 
data class LoyaltyCard(val name: String, val code: String)
 
class MainActivity : AppCompatActivity() {
 
    private val cards = mutableListOf<LoyaltyCard>()
    private lateinit var adapter: ArrayAdapter<String>
    private var tempCodeEditText: EditText? = null
 
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            tempCodeEditText?.setText(result.contents)
        }
    }
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
 
        loadCards()
 
        val listView = findViewById<ListView>(R.id.cardsListView)
        val addFab = findViewById<FloatingActionButton>(R.id.addCardFab)
 
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, cards.map { it.name })
        listView.adapter = adapter
 
        listView.setOnItemClickListener { _, _, position, _ ->
            showBarcodeDialog(cards[position])
        }
 
        listView.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("Удалить карту?")
                .setMessage("Удалить карту «${cards[position].name}»?")
                .setPositiveButton("Да") { _, _ ->
                    cards.removeAt(position)
                    saveCards()
                    updateList()
                }
                .setNegativeButton("Нет", null)
                .show()
            true
        }
 
        addFab.setOnClickListener {
            showAddCardDialog()
        }
    }
 
    private fun showAddCardDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
 
        val nameInput = EditText(this).apply { hint = "Название магазина" }
        val codeInput = EditText(this).apply { hint = "Номер карты" }
        tempCodeEditText = codeInput
 
        val scanButton = Button(this).apply {
            text = "📷 Сканировать камерой"
            setOnClickListener {
                val options = ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                    setPrompt("Наведите камеру на штрихкод карты")
                    setBeepEnabled(true)
                    setOrientationLocked(false)
                }
                barcodeLauncher.launch(options)
            }
        }
 
        layout.addView(nameInput)
        layout.addView(codeInput)
        layout.addView(scanButton)
 
        AlertDialog.Builder(this)
            .setTitle("Новая карта")
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                val name = nameInput.text.toString().trim()
                val code = codeInput.text.toString().trim()
                if (name.isNotEmpty() && code.isNotEmpty()) {
                    cards.add(LoyaltyCard(name, code))
                    saveCards()
                    updateList()
                } else {
                    Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
 
    private fun showBarcodeDialog(card: LoyaltyCard) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            gravity = android.view.Gravity.CENTER
        }
 
        val imageView = ImageView(this)
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(card.code, BarcodeFormat.CODE_128, 900, 350)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка генерации штрихкода", Toast.LENGTH_SHORT).show()
        }
 
        val codeText = TextView(this).apply {
            text = card.code
            textSize = 22f
            setPadding(0, 20, 0, 0)
            gravity = android.view.Gravity.CENTER
        }
 
        layout.addView(imageView)
        layout.addView(codeText)
 
        AlertDialog.Builder(this)
            .setTitle(card.name)
            .setView(layout)
            .setPositiveButton("Закрыть", null)
            .show()
    }
 
    private fun updateList() {
        adapter.clear()
        adapter.addAll(cards.map { it.name })
        adapter.notifyDataSetChanged()
    }
 
    private fun saveCards() {
        val prefs = getSharedPreferences("loyalty_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("cards_list", Gson().toJson(cards)).apply()
    }
 
    private fun loadCards() {
        val prefs = getSharedPreferences("loyalty_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("cards_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<LoyaltyCard>>() {}.type
            cards.clear()
            cards.addAll(Gson().fromJson(json, type))
        }
    }
}
